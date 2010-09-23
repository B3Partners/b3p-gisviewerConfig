package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Boy
 */
public class ConfigKaartGroepAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigKaartGroepAction.class);

    protected static final String HASTHEMAS_ERROR_KEY = "error.hasthemas";
    protected static final String HASCHILDCLUSTER_ERROR_KEY = "error.haschildcluster";

    public static final String CLUSTERID="clusterId";

    protected Clusters getCluster(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("clusterID"));
        Clusters c = null;
        if (id == null && createNew) {
            c = new Clusters();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            c = (Clusters) sess.get(Clusters.class, id);
        }
        return c;
    }

    protected Clusters getFirstCluster() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Clusters order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Clusters) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        request.setAttribute("allClusters", sess.createQuery("from Clusters order by naam").list());

        List ctl = SpatialUtil.getValidClusters();
        Map rootClusterMap = getClusterMap(ctl, null);

        List actieveClusters=null;
        if (FormUtils.nullIfEmpty(request.getParameter(CLUSTERID))!=null){
            actieveClusters=new ArrayList();
            String[] ids=request.getParameter(CLUSTERID).split(",");
            for (int i =0; i < ids.length; i++){
                try{
                    int id= Integer.parseInt(ids[i]);
                    actieveClusters.add(id);
                }catch (NumberFormatException nfe){
                    logger.error("ClusterId geen integer. ",nfe);
                }
            }
            if (actieveClusters.isEmpty()){
                actieveClusters=null;
            }
        }

        //request.setAttribute("tree", createJasonObject(rootClusterMap, actieveClusters));
        request.setAttribute("tree", null);
    }

    protected JSONObject createJasonObject(Map rootClusterMap, List actieveClusters) throws JSONException {
        JSONObject root = new JSONObject().put("id", "root").put("type", "root").put("title", "root");
        if (rootClusterMap == null || rootClusterMap.isEmpty()) {
            return root;
        }
        List clusterMaps = (List) rootClusterMap.get("subclusters");
        if (clusterMaps == null || clusterMaps.isEmpty()) {
            return root;
        }

        //root.put("children", getSubClusters(clusterMaps, null, actieveClusters));

        return root;
    }

    private Map getClusterMap(List clusterlist, Clusters rootCluster) throws JSONException, Exception {
        if (clusterlist == null) {
            return null;
        }

        List subclusters = null;
        Iterator it = clusterlist.iterator();
        while (it.hasNext()) {
            Clusters cluster = (Clusters) it.next();
            if (rootCluster == cluster.getParent()) {
                Map clusterMap = getClusterMap(clusterlist, cluster);
                if (clusterMap == null || clusterMap.isEmpty()) {
                    continue;
                }
                if (subclusters == null) {
                    subclusters = new ArrayList();
                }
                subclusters.add(clusterMap);
            }
        }

        if (((subclusters == null || subclusters.isEmpty()))) {
            return null;
        }
        Map clusterNode = new HashMap();
        clusterNode.put("subclusters", subclusters);
        clusterNode.put("cluster", rootCluster);

        return clusterNode;
    }

    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Clusters c = getCluster(dynaForm, false);
        if (c == null) {
            c = getFirstCluster();
        }
        populateClustersForm(c, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Clusters c = getCluster(dynaForm, false);
        if (c == null) {
            c = getFirstCluster();
        }
        populateClustersForm(c, dynaForm, request);
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Clusters c = getCluster(dynaForm, true);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateClustersObject(dynaForm, c, request);

        sess.saveOrUpdate(c);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(c);
        populateClustersForm(c, dynaForm, request);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Clusters c = getCluster(dynaForm, false);
        if (c == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* indien nog themas of children dan niet wissen, levert
         * ConstraintViolationException op */
        int themaSize = c.getThemas().size();
        int childrenSize = c.getChildren().size();

        if (themaSize > 0) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, HASTHEMAS_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        if (childrenSize > 0) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, HASCHILDCLUSTER_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(c);
        sess.flush();

        dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private void populateClustersForm(Clusters c, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (c == null) {
            return;
        }
        dynaForm.set("clusterID", Integer.toString(c.getId().intValue()));
        dynaForm.set("naam", c.getNaam());
        dynaForm.set("omschrijving", c.getOmschrijving());
        dynaForm.set("belangnr", FormUtils.IntToString(c.getBelangnr()));
        dynaForm.set("metadatalink",c.getMetadatalink());
        dynaForm.set("default_cluster", new Boolean(c.isDefault_cluster()));
        dynaForm.set("hide_legend", new Boolean(c.isHide_legend()));
        dynaForm.set("hide_tree", new Boolean(c.isHide_tree()));
        dynaForm.set("background_cluster", new Boolean(c.isBackground_cluster()));
        dynaForm.set("extra_level", new Boolean(c.isExtra_level()));
        dynaForm.set("callable", new Boolean(c.isCallable()));
        dynaForm.set("default_visible", new Boolean (c.isDefault_visible()));
        String val = "";
        if (c.getParent() != null) {
            val = Integer.toString(c.getParent().getId().intValue());
        }
        dynaForm.set("parentID", val);
    }

    private void populateClustersObject(DynaValidatorForm dynaForm, Clusters c, HttpServletRequest request) {

        c.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        c.setOmschrijving(FormUtils.nullIfEmpty(dynaForm.getString("omschrijving")));
        if (dynaForm.getString("belangnr") != null && dynaForm.getString("belangnr").length() > 0) {
            c.setBelangnr(Integer.parseInt(dynaForm.getString("belangnr")));
        }
        c.setMetadatalink(FormUtils.nullIfEmpty(dynaForm.getString("metadatalink")));
        Boolean b = (Boolean) dynaForm.get("default_cluster");
        c.setDefault_cluster(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("hide_legend");
        c.setHide_legend(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("hide_tree");
        c.setHide_tree(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("background_cluster");
        c.setBackground_cluster(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("extra_level");
        c.setExtra_level(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("callable");
        c.setCallable(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("default_visible");
        c.setDefault_visible(b == null ? false : b.booleanValue());

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        String parentID = FormUtils.nullIfEmpty(dynaForm.getString("parentID"));
        if (parentID != null) {
            int mId = 0;
            try {
                mId = Integer.parseInt(dynaForm.getString("parentID"));
            } catch (NumberFormatException ex) {
                logger.error("Illegal parent id", ex);
            }
            Clusters m = (Clusters) sess.get(Clusters.class, new Integer(mId));
            c.setParent(m);
        }else{
            c.setParent(null);
        }
    }
}
