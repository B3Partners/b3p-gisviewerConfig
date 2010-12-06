package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.gis.utils.ConfigListsUtil;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Boy
 */
public class ConfigGegevensbronAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigGegevensbronAction.class);

    protected static final String HASTHEMAS_ERROR_KEY = "error.hasthemas";
    protected static final String HASCHILDGEGVNBRON_ERROR_KEY = "error.haschildgb";

    protected static final String ERROR_ISPARENT = "error.gb.isparent";

    public static final String GEGEVENSBRONID = "gegevensbronID";

    protected Gegevensbron getGegevensbron(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("gegevensbronID"));
        Gegevensbron gb = null;

        if (id == null && createNew) {
            gb = new Gegevensbron();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            gb = (Gegevensbron) sess.get(Gegevensbron.class, id);
        }

        return gb;
    }

    protected Gegevensbron getFirstGegevensbron() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List cs = sess.createQuery("from Gegevensbron order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Gegevensbron) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        super.createLists(form, request);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List gegevensbronnen = sess.createQuery("from Gegevensbron order by volgordenr, naam").list();
        request.setAttribute("alleGegevensbronnen", gegevensbronnen);

        List bronnen = sess.createQuery("from Bron order by naam").list();
        request.setAttribute("listBronnen", bronnen);
        
        /* vullen velden voor gegevensbron */
        List tns = new ArrayList();
        Bron b = null;

        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        String tmp = (String) form.getString("bron");
        Integer bronId = null;

        if (tmp != null) {
            try {
                bronId = new Integer(tmp);
            } catch (NumberFormatException nfe) {
                logger.debug("NumberFormatException: No bronid found in Gegevensbron form.");
            }
        }

        try {
            b = ConfigListsUtil.getBron(sess, user, bronId);
            tns = ConfigListsUtil.getPossibleFeatures(b);
        } catch (Exception e) {
            logger.error("", e);
        }

        request.setAttribute("listTables", tns);

        String adminTable = null;

        adminTable = FormUtils.nullIfEmpty(form.getString("admin_tabel"));

        Gegevensbron gb = getGegevensbron(form, false);

        if (adminTable == null && gb != null) {
            adminTable = gb.getAdmin_tabel();
        }

        if (adminTable != null) {
            List atc = ConfigListsUtil.getPossibleAttributes(b, adminTable);
            request.setAttribute("listAdminTableColumns", atc);
        }

        /* opbouwen boom */
        request.setAttribute("tree", createJasonObject().toString());
    }

    protected JSONObject createJasonObject() throws JSONException, Exception {
        JSONObject root = new JSONObject();

        root.put("id", "0");
        root.put("title", "root");
        root.put("name", "root");

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List ctl = sess.createQuery("from Gegevensbron order by volgordenr, naam").list();

        Map rootGbMap = getGegevensbronMap(ctl, null);
        List gbMaps = (List) rootGbMap.get("subbronnen");

        root.put("children", getSubBronnen(gbMaps, null));

        return root;
    }

    private JSONArray getSubBronnen(List subBronnen, JSONArray bronnenArray) throws JSONException {
        if (subBronnen == null) {
            return bronnenArray;
        }

        Iterator it = subBronnen.iterator();
        while (it.hasNext()) {
            Map clMap = (Map) it.next();

            Gegevensbron gb = (Gegevensbron) clMap.get("gegevensbron");

            JSONObject jsonGb = new JSONObject();

            jsonGb.put("id", gb.getId().intValue());
            jsonGb.put("title", gb.getNaam());
            jsonGb.put("name", gb.getNaam());

            List subsubMaps = (List) clMap.get("subbronnen");

            if (subsubMaps != null && !subsubMaps.isEmpty()) {
                JSONArray childrenArray = new JSONArray();

                childrenArray = getSubBronnen(subsubMaps, childrenArray);
                jsonGb.put("children", childrenArray);
            }

            if (bronnenArray == null) {
                bronnenArray = new JSONArray();
            }

            bronnenArray.put(jsonGb);
        }

        return bronnenArray;
    }

    private Map getGegevensbronMap(List gbList, Gegevensbron rootGb) throws JSONException, Exception {
        if (gbList == null) {
            return null;
        }

        List subBronnen = null;
        Iterator it = gbList.iterator();
        while (it.hasNext()) {
            Gegevensbron gb = (Gegevensbron) it.next();
            if (rootGb == gb.getParent()) {
                Map gbMap = getGegevensbronMap(gbList, gb);
                if (gbMap == null || gbMap.isEmpty()) {
                    continue;
                }
                if (subBronnen == null) {
                    subBronnen = new ArrayList();
                }
                subBronnen.add(gbMap);
            }
        }

        Map gbNode = new HashMap();
        gbNode.put("subbronnen", subBronnen);
        gbNode.put("gegevensbron", rootGb);

        return gbNode;
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(dynaForm, false);
        if (gb == null) {
            gb = getFirstGegevensbron();
        }

        populateGegevensbronForm(gb, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Gegevensbron gb = getGegevensbron(dynaForm, false);
        if (gb == null) {
            gb = getFirstGegevensbron();
        }
        populateGegevensbronForm(gb, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Gegevensbron gb = getGegevensbron(dynaForm, true);
        if (gb == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* check of parent verplaatst wordt naar onderliggende gegevensbron */
        String parentIdString = FormUtils.nullIfEmpty(dynaForm.getString("parentID"));

        if (parentIdString != null) {
            int parentId = 0;
            try {
                parentId = Integer.parseInt(dynaForm.getString("parentID"));
            } catch (NumberFormatException ex) {
                logger.error("Illegal parent id", ex);
            }

            if (parentId > 0) {
                int rootId = getRootParentId(parentId);

                if (gb.getId() != null && gb.getId() == rootId) {
                    prepareMethod(dynaForm, request, LIST, EDIT);
                    addAlternateMessage(mapping, request, ERROR_ISPARENT);

                    return getAlternateForward(mapping, request);
                }
            }
        }

        populateGegevensbronObject(dynaForm, gb, request);

        sess.saveOrUpdate(gb);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(gb);
        populateGegevensbronForm(gb, dynaForm, request);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private int getRootParentId(int id) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Gegevensbron gb = (Gegevensbron) sess.get(Gegevensbron.class, id);

        if (gb.getParent() != null) {
            return getRootParentId(gb.getParent().getId());
        }

        return gb.getId();
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Gegevensbron gb = getGegevensbron(dynaForm, false);
        if (gb == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* indien nog themas of children dan niet wissen, levert
         * ConstraintViolationException op */
        int themaSize = gb.getThemas().size();
        int childrenSize = gb.getChildren().size();

        if (themaSize > 0) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, HASTHEMAS_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        if (childrenSize > 0) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, HASCHILDGEGVNBRON_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(gb);
        sess.flush();

        dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }
    
    private void populateGegevensbronForm(Gegevensbron gb, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (gb == null) {
            return;
        }
        
        dynaForm.set("gegevensbronID", Integer.toString(gb.getId().intValue()));
        dynaForm.set("naam", gb.getNaam());

        String valBron = "-1";
        String adminTable = gb.getAdmin_tabel();
        if (adminTable != null && adminTable.length() > 0) {
            // adminTable kan alleen een waarde hebben, als er een connectie is.
            valBron = "0";
            if (gb.getBron() != null) {
                valBron = Integer.toString(gb.getBron().getId());
            }
        }
        dynaForm.set("bron", valBron);

        dynaForm.set("admin_tabel", gb.getAdmin_tabel());
        dynaForm.set("admin_pk", gb.getAdmin_pk());
        dynaForm.set("admin_pk", gb.getAdmin_pk());
        
        String val = "";
        if (gb.getParent() != null) {
            val = Integer.toString(gb.getParent().getId().intValue());
        }
        dynaForm.set("parentID", val);
        
        dynaForm.set("admin_fk", gb.getAdmin_fk());
        dynaForm.set("admin_query", gb.getAdmin_query());
        dynaForm.set("admin_tabel_opmerkingen", gb.getAdmin_tabel_opmerkingen());

        if (gb.getVolgordenr() != null)
            dynaForm.set("volgordenr", FormUtils.IntToString(gb.getVolgordenr()));
    }

    private void populateGegevensbronObject(DynaValidatorForm dynaForm, Gegevensbron gb, HttpServletRequest request) {

        gb.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Bron b = null;
        int bronId = -1;
        try {
            bronId = Integer.parseInt(dynaForm.getString("bron"));
        } catch (NumberFormatException nfe) {
            logger.debug("No bron id found in form, input: " + dynaForm.getString("bron"));
        }
        if (bronId > 0) {
            b = (Bron) sess.get(Bron.class, bronId);
        }

        gb.setBron(b);

        gb.setAdmin_tabel(FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel")));
        gb.setAdmin_pk(FormUtils.nullIfEmpty(dynaForm.getString("admin_pk")));

        String parentID = FormUtils.nullIfEmpty(dynaForm.getString("parentID"));
        if (parentID != null) {
            int mId = 0;
            try {
                mId = Integer.parseInt(dynaForm.getString("parentID"));
            } catch (NumberFormatException ex) {
                logger.error("Illegal parent id", ex);
            }
            Gegevensbron m = (Gegevensbron) sess.get(Gegevensbron.class, new Integer(mId));
            gb.setParent(m);
        } else {
            gb.setParent(null);
        }

        if (dynaForm.getString("volgordenr") != null && dynaForm.getString("volgordenr").length() > 0) {
            gb.setVolgordenr(Integer.parseInt(dynaForm.getString("volgordenr")));
        } else {
            gb.setVolgordenr(null);
        }

        gb.setAdmin_fk(FormUtils.nullIfEmpty(dynaForm.getString("admin_fk")));
        gb.setAdmin_query(FormUtils.nullIfEmpty(dynaForm.getString("admin_query")));
        gb.setAdmin_tabel_opmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel_opmerkingen")));
    }
}
