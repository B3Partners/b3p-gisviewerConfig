package nl.b3p.gis.viewer;

import nl.b3p.gis.utils.ConfigListsUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.viewer.db.Clusters;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SpatialUtil;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class ConfigThemaAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigThemaAction.class);
    private static final String REFRESHLISTS = "refreshLists";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();
        ExtendedMethodProperties crudProp = new ExtendedMethodProperties(REFRESHLISTS);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setAlternateForwardName(FAILURE);
        map.put(REFRESHLISTS, crudProp);
        return map;
    }

    public ActionForward refreshLists(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        return mapping.findForward(SUCCESS);

    }

    protected Themas getThema(DynaValidatorForm form, boolean createNew) {
        Integer id = FormUtils.StringToInteger(form.getString("themaID"));
        Themas t = null;
        if (id == null && createNew) {
            t = new Themas();
        } else if (id != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            t = (Themas) sess.get(Themas.class, id);
        }
        return t;
    }

    protected Themas getFirstThema() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List cs = sess.createQuery("from Themas order by naam").setMaxResults(1).list();
        if (cs != null && cs.size() > 0) {
            return (Themas) cs.get(0);
        }
        return null;
    }

    @Override
    protected void createLists(DynaValidatorForm dynaForm, HttpServletRequest request) throws Exception {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        request.setAttribute("allThemas", sess.createQuery("from Themas order by naam").list());
        request.setAttribute("allClusters", sess.createQuery("from Clusters where default_cluster=:defaultCluster order by naam").setBoolean("defaultCluster", false).list());
        request.setAttribute("listValidGeoms", SpatialUtil.VALID_GEOMS);
        request.setAttribute("listConnecties", sess.createQuery("from Bron order by naam").list());

        Themas t = getThema(dynaForm, false);
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Integer cId = new Integer(-1);

        try {
            cId = new Integer(dynaForm.getString("connectie"));
        } catch (NumberFormatException nfe) {
            logger.debug("No connection id found in form, input: " + dynaForm.getString("connectie"));
        }

        if (user != null) {
            List lns = user.getLayers(false, true);
            request.setAttribute("listLayers", lns);

            List llns = new ArrayList();
            llns = user.getLayers(true, true);
            request.setAttribute("listLegendLayers", llns);
        }

        /* vullen velden voor gegevensbron en geavanceerd */
        List tns = new ArrayList();
        Bron b = null;

        try {
            b = ConfigListsUtil.getBron(sess, user, cId);
            tns = ConfigListsUtil.getPossibleFeatures(b);
        } catch (Exception e) {
            logger.error("", e);
        }

        request.setAttribute("listTables", tns);

        String adminTable = null;
        String spatialTable = null;

        adminTable = FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel"));
        spatialTable = FormUtils.nullIfEmpty(dynaForm.getString("spatial_tabel"));

        if (adminTable == null && t != null) {
            adminTable = t.getAdmin_tabel();
        }

        if (spatialTable == null && t != null) {
            spatialTable = t.getSpatial_tabel();
        }

        if (adminTable != null) {
            List atc = ConfigListsUtil.getPossibleAttributes(b, adminTable);
            request.setAttribute("listAdminTableColumns", atc);
        }

        if (spatialTable != null) {
            List stc = ConfigListsUtil.getPossibleAttributes(b, spatialTable);
            request.setAttribute("listSpatialTableColumns", stc);
        }
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        populateThemasForm(t, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Themas t = getThema(dynaForm, false);
        if (t == null) {
            t = getFirstThema();
        }
        populateThemasForm(t, dynaForm, request);
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

        // nieuwe default actie op delete zetten
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ActionErrors errors = dynaForm.validate(mapping, request);
        if (!errors.isEmpty()) {
            addMessages(request, errors);
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, VALIDATION_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        Themas t = getThema(dynaForm, true);
        if (t == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        populateThemasObject(dynaForm, t, request);

        sess.saveOrUpdate(t);
        sess.flush();

        /* Indien we input bijvoorbeeld herformatteren oid laad het dynaForm met
         * de waardes uit de database.
         */
        sess.refresh(t);


        populateThemasForm(t, dynaForm, request);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
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

        Themas t = getThema(dynaForm, false);
        if (t == null) {
            prepareMethod(dynaForm, request, LIST, EDIT);
            addAlternateMessage(mapping, request, NOTFOUND_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        sess.delete(t);
        sess.flush();

        dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    private void populateThemasForm(Themas t, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (t == null) {
            return;
        }
        dynaForm.set("themaID", Integer.toString(t.getId()));
        dynaForm.set("themaCode", t.getCode());
        dynaForm.set("naam", t.getNaam());
        dynaForm.set("metadatalink", t.getMetadata_link());

        String valConnectie = "-1";
        String adminTable = t.getAdmin_tabel();
        if (adminTable != null && adminTable.length() > 0) {
            // adminTable kan alleen een waarde hebben, als er een connectie is.
            valConnectie = "0";
            if (t.getConnectie() != null) {
                valConnectie = Integer.toString(t.getConnectie().getId());
            }
        }
        dynaForm.set("connectie", valConnectie);
        dynaForm.set("belangnr", FormUtils.IntToString(t.getBelangnr()));
        String valCluster = "";
        if (t.getCluster() != null) {
            valCluster = Integer.toString(t.getCluster().getId().intValue());
        }
        dynaForm.set("clusterID", valCluster);
        dynaForm.set("opmerkingen", t.getOpmerkingen());
        dynaForm.set("analyse_thema", new Boolean(t.isAnalyse_thema()));
        dynaForm.set("locatie_thema", new Boolean(t.isLocatie_thema()));

        dynaForm.set("admin_tabel_opmerkingen", t.getAdmin_tabel_opmerkingen());
        dynaForm.set("admin_tabel", t.getAdmin_tabel());
        dynaForm.set("admin_pk", t.getAdmin_pk());
        dynaForm.set("admin_pk_complex", new Boolean(t.isAdmin_pk_complex()));
        dynaForm.set("admin_spatial_ref", t.getAdmin_spatial_ref());

        dynaForm.set("admin_query", t.getAdmin_query());        

        dynaForm.set("spatial_tabel_opmerkingen", t.getSpatial_tabel_opmerkingen());
        dynaForm.set("spatial_tabel", t.getSpatial_tabel());
        dynaForm.set("spatial_pk", t.getSpatial_pk());
        dynaForm.set("spatial_pk_complex", new Boolean(t.isSpatial_pk_complex()));
        dynaForm.set("spatial_admin_ref", t.getSpatial_admin_ref());
        dynaForm.set("wms_url", t.getWms_url());
        dynaForm.set("wms_layers", t.getWms_layers());
        dynaForm.set("wms_layers_real", t.getWms_layers_real());
        dynaForm.set("wms_querylayers", t.getWms_querylayers());
        dynaForm.set("wms_querylayers_real", t.getWms_querylayers_real());
        dynaForm.set("wms_legendlayer", t.getWms_legendlayer());
        dynaForm.set("wms_legendlayer_real", t.getWms_legendlayer_real());
        dynaForm.set("thema_maptip", t.getMaptipstring());
        dynaForm.set("update_frequentie_in_dagen", FormUtils.IntegerToString(t.getUpdate_frequentie_in_dagen()));
        dynaForm.set("view_geomtype", t.getView_geomtype());
        dynaForm.set("visible", new Boolean(t.isVisible()));
        dynaForm.set("sldattribuut",t.getSldattribuut());
        dynaForm.set("uitgebreid", new Boolean(t.isUitgebreid()));
        dynaForm.set("layoutadmindata",t.getLayoutadmindata());

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List themadataobjecten = sess.createQuery("select kolomnaam from ThemaData where thema = :thema").setEntity("thema", t).list();
        dynaForm.set("themadataobjecten", themadataobjecten.toArray(new String[themadataobjecten.size()]));
    }

    private void populateThemasObject(DynaValidatorForm dynaForm, Themas t, HttpServletRequest request) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        t.setCode(FormUtils.nullIfEmpty(dynaForm.getString("themaCode")));
        t.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        t.setMetadata_link(FormUtils.nullIfEmpty(dynaForm.getString("metadatalink")));
        Bron conn = null;
        int connId = -1;
        try {
            connId = Integer.parseInt(dynaForm.getString("connectie"));
        } catch (NumberFormatException nfe) {
            logger.debug("No connection id found in form, input: " + dynaForm.getString("connectie"));
        }
        if (connId > 0) {
            conn = (Bron) sess.get(Bron.class, connId);
        }
        t.setConnectie(conn);
        if (dynaForm.getString("belangnr") != null && dynaForm.getString("belangnr").length() > 0) {
            t.setBelangnr(Integer.parseInt(dynaForm.getString("belangnr")));
        }
        t.setOpmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("opmerkingen")));
        Boolean b = (Boolean) dynaForm.get("analyse_thema");
        t.setAnalyse_thema(b == null ? false : b.booleanValue());
        b = (Boolean) dynaForm.get("locatie_thema");
        t.setLocatie_thema(b == null ? false : b.booleanValue());
        t.setAdmin_tabel_opmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel_opmerkingen")));
        t.setAdmin_tabel(FormUtils.nullIfEmpty(dynaForm.getString("admin_tabel")));
        t.setAdmin_pk(FormUtils.nullIfEmpty(dynaForm.getString("admin_pk")));
        b = (Boolean) dynaForm.get("admin_pk_complex");
        t.setAdmin_pk_complex(b == null ? false : b.booleanValue());
        t.setAdmin_spatial_ref(FormUtils.nullIfEmpty(dynaForm.getString("admin_spatial_ref")));
        t.setAdmin_query(FormUtils.nullIfEmpty(dynaForm.getString("admin_query")));
        t.setSpatial_tabel_opmerkingen(FormUtils.nullIfEmpty(dynaForm.getString("spatial_tabel_opmerkingen")));
        t.setSpatial_tabel(FormUtils.nullIfEmpty(dynaForm.getString("spatial_tabel")));
        t.setSpatial_pk(FormUtils.nullIfEmpty(dynaForm.getString("spatial_pk")));
        b = (Boolean) dynaForm.get("spatial_pk_complex");
        t.setSpatial_pk_complex(b == null ? false : b.booleanValue());
        t.setSpatial_admin_ref(FormUtils.nullIfEmpty(dynaForm.getString("spatial_admin_ref")));
        t.setMaptipstring(FormUtils.nullIfEmpty(dynaForm.getString("thema_maptip")));
        t.setWms_url(FormUtils.nullIfEmpty(dynaForm.getString("wms_url")));
        //komma separated layers
        t.setWms_layers(FormUtils.nullIfEmpty(dynaForm.getString("wms_layers")));
        t.setWms_layers_real(FormUtils.nullIfEmpty(dynaForm.getString("wms_layers_real")));
        //komma separated layers
        t.setWms_querylayers(FormUtils.nullIfEmpty(dynaForm.getString("wms_querylayers")));
        t.setWms_querylayers_real(FormUtils.nullIfEmpty(dynaForm.getString("wms_querylayers_real")));
        //one layer to create a wms legend image
        t.setWms_legendlayer(FormUtils.nullIfEmpty(dynaForm.getString("wms_legendlayer")));
        t.setWms_legendlayer_real(FormUtils.nullIfEmpty(dynaForm.getString("wms_legendlayer_real")));
        t.setUpdate_frequentie_in_dagen(FormUtils.StringToInteger(dynaForm.getString("update_frequentie_in_dagen")));
        t.setView_geomtype(FormUtils.nullIfEmpty(dynaForm.getString("view_geomtype")));
        b = (Boolean) dynaForm.get("visible");
        t.setVisible(b == null ? false : b.booleanValue());
        t.setSldattribuut(FormUtils.nullIfEmpty(dynaForm.getString("sldattribuut")));
        b = (Boolean) dynaForm.get("uitgebreid");
        t.setUitgebreid(b == null ? false : b.booleanValue());
        t.setLayoutadmindata(FormUtils.nullIfEmpty(dynaForm.getString("layoutadmindata")));

        int cId = -1;
        try {
            cId = Integer.parseInt(dynaForm.getString("clusterID"));
        } catch (NumberFormatException ex) {
            logger.error("Illegal Cluster id", ex);
        }
        Clusters c = (Clusters) sess.get(Clusters.class, new Integer(cId));
        t.setCluster(c);
    }
}
