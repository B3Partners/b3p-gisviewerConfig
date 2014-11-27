package nl.b3p.gis.viewer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.geotools.DataStoreUtil;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.db.Gegevensbron;
import nl.b3p.gis.viewer.db.Themas;
import nl.b3p.gis.viewer.db.UserLayer;
import nl.b3p.gis.viewer.db.UserService;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import nl.b3p.gis.viewer.services.SldServlet;
import nl.b3p.zoeker.configuratie.Bron;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.geotools.data.wms.WMSUtils;
import org.geotools.data.wms.WebMapServer;
import org.hibernate.Session;
import org.json.JSONException;

public class ConfigKeeperAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigKeeperAction.class);

    private static final String[] CONFIGKEEPER_TABS = {
        "leeg", "themas", "legenda", "zoeken", "gebieden",
        "analyse", "planselectie", "meldingen", "redlining",
        "bag", "wkt", "transparantie", "tekenen",
        "uploadpoints", "layerinfo"
    };

    private static final String[] LABELS_VOOR_TABS = {
        "-Kies een tabblad-", "Kaarten", "Legenda", "Zoeken", "Gebieden",
        "Analyse", "Plannen", "Meldingen", "Redlining",
        "BAG", "WKT", "Transparantie", "Tekenen",
        "Upload tijdelijke punten", "Laag informatie"
    };

    private static final String[] CONFIGKEEPER_SLIDER_TABS = {
        "leeg", "themas", "legenda", "zoeken"
    };

    private static final String[] LABELS_VOOR_SLIDER_TABS = {
        "-Kies een tabblad-", "Kaarten", "Legenda", "Zoeken"
    };

    protected static final String RESET_INSTELLINGEN = "resetInstellingen";

    protected static final String SAVE_WMS_SERVICE = "saveWMSService";
    protected static final String DELETE_WMS_SERVICES = "deleteWMSServices";

    protected static final String ERROR_SAVE_WMS = "error.save.wms";
    protected static final String ERROR_DUPLICATE_WMS = "error.duplicate.wms";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(SAVE);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.saveinstellingen.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.saveinstellingen.failed");
        map.put(SAVE, crudProp);

        crudProp = new ExtendedMethodProperties(RESET_INSTELLINGEN);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.resetappsettings.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.resetappsettings.failed");
        map.put(RESET_INSTELLINGEN, crudProp);

        crudProp = new ExtendedMethodProperties(SAVE_WMS_SERVICE);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.userwms.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.userwms.failed");
        map.put(SAVE_WMS_SERVICE, crudProp);

        crudProp = new ExtendedMethodProperties(DELETE_WMS_SERVICES);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.userwms.delete.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.userwms.delete.failed");
        map.put(DELETE_WMS_SERVICES, crudProp);

        return map;
    }

    public ActionForward resetInstellingen(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return this.getAlternateForward(mapping, request);
        }

        String appCode = dynaForm.getString("appcode");

        /* configkeeper instellingen verwijderen en daarna defaults schrijven
         voor deze appcode */
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        int query = sess.createQuery("delete from Configuratie where setting = :appcode)").setParameter("appcode", appCode).executeUpdate();

        sess.flush();

        if (query > 0) {
            ConfigKeeper.writeDefaultApplicatie(appCode);
            logger.debug("Applicatieinstellingen zijn gereset voor appcode " + appCode);
        }

        /* User kaartgroepen en kaartlagen instellingen verwijderen */
        KaartSelectieUtil.removeExistingUserKaartgroepAndUserKaartlagen(appCode);
        KaartSelectieUtil.resetExistingUserLayers(appCode);

        /* Basisboom ophalen */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        /* Configkeeper instellingen ook klaarzetten voor form */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = null;
        map = configKeeper.getConfigMap(appCode);

        if (map.size() > 1) {
            populateForm(dynaForm, request, map, appCode);
        }

        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* Applicatie code ophalen */
        String appCode = (String) request.getParameter("appcode");

        /* Basisboom ophalen */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        /* Applicatieinstellingen ophalen en klaarzetten voor form */
        Map map = null;
        ConfigKeeper configKeeper = new ConfigKeeper();
        map = configKeeper.getConfigMap(appCode);

        if (map.isEmpty()) {
            ConfigKeeper.writeDefaultApplicatie(appCode);
            map = configKeeper.getConfigMap(appCode);
        }

        populateForm(dynaForm, request, map, appCode);

        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    public ActionForward deleteWMSServices(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String[] servicesAan = (String[]) dynaForm.get("servicesAan");
        String appCode = (String) dynaForm.get("appcode");

        /* Aangevinkte User services verwijderen */
        for (int i = 0; i < servicesAan.length; i++) {
            Integer serviceId = new Integer(servicesAan[i]);
            KaartSelectieUtil.removeService(serviceId);
        }

        /* Basisboom weer klaarzetten */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        /* Configkeeper instellingen ook klaarzetten voor form */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = null;
        map = configKeeper.getConfigMap(appCode);

        if (!map.isEmpty()) {
            populateForm(dynaForm, request, map, appCode);
        }

        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    public ActionForward saveWMSService(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String groupName = (String) dynaForm.get("groupName");
        String serviceUrl = (String) dynaForm.get("serviceUrl");
        String sldUrl = (String) dynaForm.get("sldUrl");

        boolean error = false;

        /* Controleren of User service al voorkomt bij applicatie */
        String appCode = (String) dynaForm.get("appcode");

        if (KaartSelectieUtil.userAlreadyHasThisService(appCode, serviceUrl)) {
            KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

            addMessage(request, ERROR_DUPLICATE_WMS, serviceUrl);
            error = true;
        }

        /* WMS Service layers ophalen met Geotools */
        URI uri = new URI(serviceUrl);
        org.geotools.data.ows.Layer[] layers = null;
        try {
            WebMapServer wms = new WebMapServer(uri.toURL(), 30000);
            layers = WMSUtils.getNamedLayers(wms.getCapabilities());
        } catch (Exception ex) {
            logger.error("Fout tijdens opslaan WMS. ", ex);

            /* Basisboom weer klaarzetten */
            KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

            addMessage(request, ERROR_SAVE_WMS, uri.toString());
            error = true;
        }

        if (!error) {

            /* Nieuwe UserService entity aanmaken en opslaan */
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

            UserService us = new UserService(appCode, serviceUrl, groupName);

            /* Eerst parents ophalen. */
            List<org.geotools.data.ows.Layer> parents
                    = KaartSelectieUtil.getParentLayers(layers);

            for (org.geotools.data.ows.Layer layer : parents) {
                UserLayer ul = KaartSelectieUtil.createUserLayers(us, layer, null);
                us.addLayer(ul);
            }

            /* Indien geen parents gevonden maar wel layers dan gewoon allemaal
             * toevoegen. */
            if (parents.size() < 1) {
                for (int i = 0; i < layers.length; i++) {
                    UserLayer ul = KaartSelectieUtil.createUserLayers(us, layers[i], null);
                    us.addLayer(ul);
                }
            }

            sess.save(us);
        }

        /* Basisboom weer klaarzetten */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        /* Configkeeper instellingen ook klaarzetten voor form */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = null;
        map = configKeeper.getConfigMap(appCode);

        if (map.size() > 1) {
            populateForm(dynaForm, request, map, appCode);
        }

        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);

        if (!error) {
            addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
            return mapping.findForward(SUCCESS);
        } else {
            return getAlternateForward(mapping, request);
        }
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        request.setAttribute("tabValues", CONFIGKEEPER_TABS);
        request.setAttribute("tabLabels", LABELS_VOOR_TABS);

        request.setAttribute("tabSliderValues", CONFIGKEEPER_SLIDER_TABS);
        request.setAttribute("tabSliderLabels", LABELS_VOOR_SLIDER_TABS);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();
        request.setAttribute("zoekConfigs", zoekconfigs);

        List meldingGegevensbronnen = sess.createQuery("from Gegevensbron order by naam").list();
        request.setAttribute("meldingGegevensbronnen", meldingGegevensbronnen);

        List redliningKaartlagen = sess.createQuery("from Themas order by naam").list();
        request.setAttribute("redliningKaartlagen", redliningKaartlagen);

        List<Gegevensbron> tekenGegevensbronnen = sess.createQuery("from Gegevensbron order by naam").list();
        List<Gegevensbron> tempGb = new ArrayList<Gegevensbron>();
        for (Iterator<Gegevensbron> it = tekenGegevensbronnen.iterator(); it.hasNext();) {
            Gegevensbron gegevensbron = it.next();
            if (gegevensbron.getBron() != null && gegevensbron.getBron().checkType(Bron.TYPE_JDBC)) {
                tempGb.add(gegevensbron);
            }
        }

        request.setAttribute("tekenGegevensbronnen", tempGb);

        fillTekenTabFilterKaartlagenDropdown(request, sess);

        fillTekenTabFilterSLD(request, sess, form);
    }

    private void fillTekenTabFilterKaartlagenDropdown(HttpServletRequest request,
            Session sess) throws Exception {

        String appCode = (String) request.getParameter("appcode");

        Gegevensbron gekozenGb = getTekenTabGekozenGegevensbron(appCode, sess);
        if (gekozenGb != null) {
            List<Themas> lagen = sess.createQuery("from Themas "
                    + "where gegevensbron = :gb order by naam")
                    .setParameter("gb", gekozenGb)
                    .list();

            request.setAttribute("tekenKaartlagen", lagen);
        }
    }

    private void fillTekenTabFilterSLD(HttpServletRequest request,
            Session sess, DynaValidatorForm form) throws Exception {

        // ophalen geom type en default invullen indien sld filter
        // nog niet is ingevuld
        String appCode = (String) request.getParameter("appcode");

        Themas laag = getTekenTabGekozenKaartlaag(appCode, sess);

        if (laag != null) {
            String featureType = laag.getWms_layers_real();
            if (featureType == null || featureType.length() == 0) {
                logger.debug("Kaartlaag heeft geen featuretype");
            }

            featureType = featureType.substring(featureType.indexOf("_") + 1);

            String geometryType = null;
            try {
                geometryType = getGeomtryType(laag, request);
            } catch (Exception e) {
            }

            String currentSldValue = getTekenTabFilterSLDValue(appCode);

            /* Indien onbekend standaard polygon verbeelding gebruiken */
            if (geometryType != null && currentSldValue == null || currentSldValue.equals("")) {
                logger.debug("teken kaartlaag geom type: " + geometryType);

                if (geometryType.toLowerCase().indexOf("polygon") >= 0) {
                    form.set("cfg_tekenFilterSld", SldServlet.getDefaultPolygonStyle());
                } else if (geometryType.toLowerCase().indexOf("line") >= 0) {
                    form.set("cfg_tekenFilterSld", SldServlet.getDefaultLineStyle());
                } else if (geometryType.toLowerCase().indexOf("point") >= 0) {
                    form.set("cfg_tekenFilterSld", SldServlet.getDefaultPointStyle());
                } else {
                    form.set("cfg_tekenFilterSld", SldServlet.getDefaultPolygonStyle());
                }
            }
        }
    }

    private String getGeomtryType(Themas t, HttpServletRequest request) throws Exception {
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        String geometryType = DataStoreUtil.getThemaGeomType(t, user);
        return geometryType;
    }

    private String getTekenTabFilterSLDValue(String appCode)
            throws Exception {

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(appCode);

        String sldPart = null;
        if (map != null && map.size() > 0) {
            sldPart = (String) map.get("tekenFilterSld");
        }

        return sldPart;
    }

    private Gegevensbron getTekenTabGekozenGegevensbron(String appCode,
            Session sess) throws Exception {

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(appCode);

        Gegevensbron gekozenGegevensbron = null;
        if (map != null && map.size() > 0) {
            Integer gbId = (Integer) map.get("tekenGegevensbron");

            if (gbId != null && gbId > 0) {
                gekozenGegevensbron = (Gegevensbron) sess.get(Gegevensbron.class, gbId);
            }
        }

        return gekozenGegevensbron;
    }

    private Themas getTekenTabGekozenKaartlaag(String appCode,
            Session sess) throws Exception {

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(appCode);

        Themas gekozenKaartlaag = null;
        if (map != null && map.size() > 0) {
            Integer laagId = (Integer) map.get("tekenKaartlaagId");

            if (laagId != null && laagId > 0) {
                gekozenKaartlaag = (Themas) sess.get(Themas.class, laagId);
            }
        }

        return gekozenKaartlaag;
    }

    private void populateForApplicatieHeader(HttpServletRequest request, String appCode) {
        Applicatie app = KaartSelectieUtil.getApplicatie(appCode);

        if (app != null) {
            request.setAttribute("header_appnaam", app.getNaam());
        }
    }

    public void populateForm(DynaValidatorForm dynaForm, HttpServletRequest request, Map map, String appCode) {
        Boolean useCookies = (Boolean) map.get("useCookies");
        Boolean usePopup = (Boolean) map.get("usePopup");
        Boolean useDivPopup = (Boolean) map.get("useDivPopup");
        Boolean usePanelControls = (Boolean) map.get("usePanelControls");
        Integer autoRedirect = (Integer) map.get("autoRedirect");
        Integer tolerance = (Integer) map.get("tolerance");
        Integer refreshDelay = (Integer) map.get("refreshDelay");
        String planSelectieIds = (String) map.get("planSelectieIds");
        Integer minBboxZoeken = (Integer) map.get("minBboxZoeken");
        Integer maxResults = (Integer) map.get("maxResults");
        Integer defaultSearchRadius = (Integer) map.get("defaultSearchRadius");
        Boolean expandAll = (Boolean) map.get("expandAll");
        Boolean multipleActiveThemas = (Boolean) map.get("multipleActiveThemas");
        Boolean useInheritCheckbox = (Boolean) map.get("useInheritCheckbox");
        Boolean showLegendInTree = (Boolean) map.get("showLegendInTree");
        Boolean useMouseOverTabs = (Boolean) map.get("useMouseOverTabs");
        String layoutAdminData = (String) map.get("layoutAdminData");
        Boolean showRedliningTools = (Boolean) map.get("showRedliningTools");
        Boolean showBufferTool = (Boolean) map.get("showBufferTool");
        Boolean showSelectBulkTool = (Boolean) map.get("showSelectBulkTool");
        Boolean showNeedleTool = (Boolean) map.get("showNeedleTool");
        Boolean showPrintTool = (Boolean) map.get("showPrintTool");
        Boolean showLayerSelectionTool = (Boolean) map.get("showLayerSelectionTool");
        Boolean showGPSTool = (Boolean) map.get("showGPSTool");
        Boolean edit = (Boolean) map.get("showEditTool");
        String gpsBuffer = (String) map.get("gpsBuffer");
        Boolean useUserWmsDropdown = (Boolean) map.get("useUserWmsDropdown");
        Boolean datasetDownload = (Boolean) map.get("datasetDownload");
        Boolean showServiceUrl = (Boolean) map.get("showServiceUrl");

        String layerGrouping = (String) map.get("layerGrouping");
        String popupWidth = (String) map.get("popupWidth");
        String popupHeight = (String) map.get("popupHeight");
        String popupLeft = (String) map.get("popupLeft");
        String popupTop = (String) map.get("popupTop");
        String defaultdataframehoogte = (String) map.get("defaultdataframehoogte");
        String viewerType = (String) map.get("viewerType");
        String viewerTemplate = (String) map.get("viewerTemplate");
        String objectInfoType = (String) map.get("objectInfoType");
        String treeOrder = (String) map.get("treeOrder");
        Integer tabWidth = (Integer) map.get("tabWidth");
        Integer tabWidthLeft = (Integer) map.get("tabWidthLeft");
        String extent = (String) map.get("extent");
        String fullextent = (String) map.get("fullextent");
        String activeTab = (String) map.get("activeTab");
        String transSliderTab = (String) map.get("transSliderTab");
        String tilingResolutions = (String) map.get("tilingResolutions");

        String showInfoTab = (String) map.get("showInfoTab");

        String helpUrl = (String) map.get("helpUrl");
        Boolean showGoogleMapsIcon = (Boolean) map.get("showGoogleMapsIcon");
        Boolean showBookmarkIcon = (Boolean) map.get("showBookmarkIcon");
        String contactUrl = (String) map.get("contactUrl");
        String logoutUrl = (String) map.get("logoutUrl");

        String tekenFilterColumn = (String) map.get("tekenFilterColumn");
        String tekenFilterSld = (String) map.get("tekenFilterSld");

        /* vullen box voor zoek ingangen */
        fillZoekConfigBox(dynaForm, request, map);
        fillPlanSelectieBox(dynaForm, request, planSelectieIds);

        fillMeldingenBox(dynaForm, request, map);

        /* overige settings klaarzetten voor formulier */
        dynaForm.set("cfg_useCookies", useCookies);
        dynaForm.set("cfg_autoRedirect", autoRedirect);
        dynaForm.set("cfg_tolerance", tolerance);
        dynaForm.set("cfg_refreshDelay", refreshDelay);
        dynaForm.set("cfg_minBboxZoeken", minBboxZoeken);
        dynaForm.set("cfg_maxResults", maxResults);
        dynaForm.set("cfg_defaultSearchRadius", defaultSearchRadius);
        dynaForm.set("cfg_expandAll", expandAll);
        dynaForm.set("cfg_multipleActiveThemas", multipleActiveThemas);
        dynaForm.set("cfg_useInheritCheckbox", useInheritCheckbox);
        dynaForm.set("cfg_showLegendInTree", showLegendInTree);
        dynaForm.set("cfg_useMouseOverTabs", useMouseOverTabs);
        dynaForm.set("cfg_layoutAdminData", layoutAdminData);
        dynaForm.set("cfg_showRedliningTools", showRedliningTools);
        dynaForm.set("cfg_showBufferTool", showBufferTool);
        dynaForm.set("cfg_showSelectBulkTool", showSelectBulkTool);
        dynaForm.set("cfg_showNeedleTool", showNeedleTool);
        dynaForm.set("cfg_showPrintTool", showPrintTool);
        dynaForm.set("cfg_showLayerSelectionTool", showLayerSelectionTool);
        dynaForm.set("cfg_showGPSTool", showGPSTool);
        dynaForm.set("cfg_showEditTool", edit);
        dynaForm.set("cfg_GPSBuffer", gpsBuffer);

        dynaForm.set("cfg_layerGrouping", layerGrouping);
        dynaForm.set("cfg_popupWidth", popupWidth);
        dynaForm.set("cfg_popupHeight", popupHeight);
        dynaForm.set("cfg_popupLeft", popupLeft);
        dynaForm.set("cfg_popupTop", popupTop);
        dynaForm.set("cfg_defaultdataframehoogte", defaultdataframehoogte);

        dynaForm.set("cfg_viewerType", viewerType);
        dynaForm.set("cfg_viewerTemplate", viewerTemplate);
        dynaForm.set("cfg_objectInfoType", objectInfoType);

        dynaForm.set("cfg_treeOrder", treeOrder);

        dynaForm.set("cfg_tabWidth", tabWidth);
        dynaForm.set("cfg_tabWidthLeft", tabWidthLeft);

        dynaForm.set("cfg_activeTab", activeTab);
        dynaForm.set("cfg_transSliderTab", transSliderTab);

        dynaForm.set("cfg_extent", extent);
        dynaForm.set("cfg_fullextent", fullextent);

        dynaForm.set("appcode", appCode);

        /* Tabbladen vullen */
        fillTabbladenConfig(dynaForm, map);

        /* redlining config items */
        dynaForm.set("cfg_redlininggegevensbron", (Integer) map.get("redliningGegevensbron"));
        dynaForm.set("cfg_redliningkaartlaagid", (Integer) map.get("redliningkaartlaagid"));

        /* Teken config items */
        dynaForm.set("cfg_tekenGegevensbron", (Integer) map.get("tekenGegevensbron"));
        dynaForm.set("cfg_tekenTekstBoven", (String) map.get("tekenTekstBoven"));
        dynaForm.set("cfg_tekenTekstOnder", (String) map.get("tekenTekstOnder"));
        dynaForm.set("cfg_tekenTitel", (String) map.get("tekenTitel"));
        dynaForm.set("cfg_tekenPlaatje", (String) map.get("tekenPlaatje"));

        /* Bag config items*/
        if (map.get("bagkaartlaagid") != null) {
            dynaForm.set("cfg_bagkaartlaagid", (Integer) map.get("bagkaartlaagid"));
        }
        if (map.get("bagMaxBouwjaar") != null) {
            dynaForm.set("cfg_bagmaxbouwjaar", (Integer) map.get("bagMaxBouwjaar"));
        }
        if (map.get("bagMinBouwjaar") != null) {
            dynaForm.set("cfg_bagminbouwjaar", (Integer) map.get("bagMinBouwjaar"));
        }
        if (map.get("bagMaxOpp") != null) {
            dynaForm.set("cfg_bagmaxopp", (Integer) map.get("bagMaxOpp"));
        }
        if (map.get("bagMinOpp") != null) {
            dynaForm.set("cfg_bagminopp", (Integer) map.get("bagMinOpp"));
        }
        if (map.get("bagOppAttr") != null) {
            dynaForm.set("cfg_bagOppAttr", (String) map.get("bagOppAttr"));
        }
        if (map.get("bagBouwjaarAttr") != null) {
            dynaForm.set("cfg_bagBouwjaarAttr", (String) map.get("bagBouwjaarAttr"));
        }
        if (map.get("bagGebruiksfunctieAttr") != null) {
            dynaForm.set("cfg_bagGebruiksfunctieAttr", (String) map.get("bagGebruiksfunctieAttr"));
        }
        if (map.get("bagGeomAttr") != null) {
            dynaForm.set("cfg_bagGeomAttr", (String) map.get("bagGeomAttr"));
        }

        dynaForm.set("cfg_useUserWmsDropdown", useUserWmsDropdown);
        dynaForm.set("cfg_datasetDownload", datasetDownload);
        dynaForm.set("cfg_tilingResolutions", tilingResolutions);
        dynaForm.set("cfg_showServiceUrl", showServiceUrl);

        dynaForm.set("cfg_showInfoTab", showInfoTab);

        dynaForm.set("cfg_helpUrl", helpUrl);
        dynaForm.set("cfg_showGoogleMapsIcon", showGoogleMapsIcon);
        dynaForm.set("cfg_showBookmarkIcon", showBookmarkIcon);
        dynaForm.set("cfg_contactUrl", contactUrl);
        dynaForm.set("cfg_logoutUrl", logoutUrl);

        dynaForm.set("cfg_tekenKaartlaagId", (Integer) map.get("tekenKaartlaagId"));
        dynaForm.set("cfg_tekenFilterColumn", tekenFilterColumn);
        dynaForm.set("cfg_tekenFilterSld", tekenFilterSld);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String appCode = (String) dynaForm.get("appcode");

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return this.getAlternateForward(mapping, request);
        }

        saveKaartSelectie(appCode, dynaForm, request);

        populateObject(dynaForm, appCode);

        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        populateForApplicatieHeader(request, appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    private void saveKaartSelectie(String appCode, DynaValidatorForm dynaForm,
            HttpServletRequest request) throws JSONException, Exception {

        String[] kaartgroepenAan = (String[]) dynaForm.get("kaartgroepenAan");
        String[] kaartlagenAan = (String[]) dynaForm.get("kaartlagenAan");
        String[] kaartgroepenDefaultAan = (String[]) dynaForm.get("kaartgroepenDefaultAan");
        String[] kaartlagenDefaultAan = (String[]) dynaForm.get("kaartlagenDefaultAan");
        String[] layersAan = (String[]) dynaForm.get("layersAan");
        String[] layersDefaultAan = (String[]) dynaForm.get("layersDefaultAan");
        String[] useLayerStyles = (String[]) dynaForm.get("useLayerStyles");

        /* userLayerIds wordt gebruikt om de layerid's te kunnen koppelen
         aan de textarea's */
        String[] userLayerIds = (String[]) dynaForm.get("userLayerIds");
        String[] useLayerSldParts = (String[]) dynaForm.get("useLayerSldParts");

        /* groepen en lagen die default aan staan ook toevoegen aan arrays */
        kaartgroepenAan = KaartSelectieUtil.addDefaultOnValues(kaartgroepenDefaultAan, kaartgroepenAan);
        kaartlagenAan = KaartSelectieUtil.addDefaultOnValues(kaartlagenDefaultAan, kaartlagenAan);
        layersAan = KaartSelectieUtil.addDefaultOnValues(layersDefaultAan, layersAan);

        /* Eerst alle huidige records verwijderen. Dan hoeven we geen
         * onoverzichtelijke if meuk toe te voegen om te kijken of er vinkjes
         * ergens wel of niet aan staan en dan wissen */
        KaartSelectieUtil.removeExistingUserKaartgroepAndUserKaartlagen(appCode);
        KaartSelectieUtil.resetExistingUserLayers(appCode);

        /* Opslaan basisboom */
        KaartSelectieUtil.saveKaartGroepen(appCode, kaartgroepenAan, kaartgroepenDefaultAan);
        KaartSelectieUtil.saveKaartlagen(appCode, kaartlagenAan, kaartlagenDefaultAan);
        KaartSelectieUtil.saveServiceLayers(layersAan, layersDefaultAan);
        KaartSelectieUtil.saveUserLayerStyles(useLayerStyles);
        KaartSelectieUtil.saveUserLayerSldParts(userLayerIds, useLayerSldParts);
    }

    public void populateObject(DynaValidatorForm dynaForm, String appCode) {

        /* opslaan overige settings */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie c = null;

        /* Opslaan tabbladen */
        writeTabbladenConfig(dynaForm, appCode);

        c = configKeeper.getConfiguratie("useCookies", appCode);
        writeBoolean(dynaForm, "cfg_useCookies", c);

        c = configKeeper.getConfiguratie("autoRedirect", appCode);
        writeInteger(dynaForm, "cfg_autoRedirect", c);

        c = configKeeper.getConfiguratie("tolerance", appCode);
        writeInteger(dynaForm, "cfg_tolerance", c);

        c = configKeeper.getConfiguratie("refreshDelay", appCode);
        writeInteger(dynaForm, "cfg_refreshDelay", c);

        c = configKeeper.getConfiguratie("minBboxZoeken", appCode);
        writeInteger(dynaForm, "cfg_minBboxZoeken", c);

        c = configKeeper.getConfiguratie("maxResults", appCode);
        writeInteger(dynaForm, "cfg_maxResults", c);

        c = configKeeper.getConfiguratie("defaultSearchRadius", appCode);
        writeInteger(dynaForm, "cfg_defaultSearchRadius", c);

        c = configKeeper.getConfiguratie("expandAll", appCode);
        writeBoolean(dynaForm, "cfg_expandAll", c);

        c = configKeeper.getConfiguratie("multipleActiveThemas", appCode);
        writeBoolean(dynaForm, "cfg_multipleActiveThemas", c);

        c = configKeeper.getConfiguratie("useInheritCheckbox", appCode);
        writeBoolean(dynaForm, "cfg_useInheritCheckbox", c);

        c = configKeeper.getConfiguratie("showLegendInTree", appCode);
        writeBoolean(dynaForm, "cfg_showLegendInTree", c);

        c = configKeeper.getConfiguratie("useMouseOverTabs", appCode);
        writeBoolean(dynaForm, "cfg_useMouseOverTabs", c);

        c = configKeeper.getConfiguratie("layoutAdminData", appCode);
        writeString(dynaForm, "cfg_layoutAdminData", c);

        c = configKeeper.getConfiguratie("showRedliningTools", appCode);
        writeBoolean(dynaForm, "cfg_showRedliningTools", c);

        c = configKeeper.getConfiguratie("showBufferTool", appCode);
        writeBoolean(dynaForm, "cfg_showBufferTool", c);

        c = configKeeper.getConfiguratie("showSelectBulkTool", appCode);
        writeBoolean(dynaForm, "cfg_showSelectBulkTool", c);

        c = configKeeper.getConfiguratie("showNeedleTool", appCode);
        writeBoolean(dynaForm, "cfg_showNeedleTool", c);

        c = configKeeper.getConfiguratie("showPrintTool", appCode);
        writeBoolean(dynaForm, "cfg_showPrintTool", c);

        c = configKeeper.getConfiguratie("showLayerSelectionTool", appCode);
        writeBoolean(dynaForm, "cfg_showLayerSelectionTool", c);

        c = configKeeper.getConfiguratie("showGPSTool", appCode);
        writeBoolean(dynaForm, "cfg_showGPSTool", c);

        c = configKeeper.getConfiguratie("showEditTool", appCode);
        writeBoolean(dynaForm, "cfg_showEditTool", c);

        c = configKeeper.getConfiguratie("gpsBuffer", appCode);
        writeString(dynaForm, "cfg_GPSBuffer", c);

        c = configKeeper.getConfiguratie("layerGrouping", appCode);
        writeString(dynaForm, "cfg_layerGrouping", c);

        c = configKeeper.getConfiguratie("popupWidth", appCode);
        writeString(dynaForm, "cfg_popupWidth", c);

        c = configKeeper.getConfiguratie("popupHeight", appCode);
        writeString(dynaForm, "cfg_popupHeight", c);

        c = configKeeper.getConfiguratie("popupLeft", appCode);
        writeString(dynaForm, "cfg_popupLeft", c);

        c = configKeeper.getConfiguratie("popupTop", appCode);
        writeString(dynaForm, "cfg_popupTop", c);

        c = configKeeper.getConfiguratie("defaultdataframehoogte", appCode);
        writeString(dynaForm, "cfg_defaultdataframehoogte", c);

        c = configKeeper.getConfiguratie("viewerType", appCode);
        writeString(dynaForm, "cfg_viewerType", c);

        c = configKeeper.getConfiguratie("viewerTemplate", appCode);
        writeString(dynaForm, "cfg_viewerTemplate", c);

        c = configKeeper.getConfiguratie("objectInfoType", appCode);
        writeString(dynaForm, "cfg_objectInfoType", c);

        c = configKeeper.getConfiguratie("treeOrder", appCode);
        writeString(dynaForm, "cfg_treeOrder", c);

        c = configKeeper.getConfiguratie("tabWidth", appCode);
        writeInteger(dynaForm, "cfg_tabWidth", c);

        c = configKeeper.getConfiguratie("tabWidthLeft", appCode);
        writeInteger(dynaForm, "cfg_tabWidthLeft", c);

        c = configKeeper.getConfiguratie("activeTab", appCode);
        writeString(dynaForm, "cfg_activeTab", c);

        c = configKeeper.getConfiguratie("transSliderTab", appCode);
        writeString(dynaForm, "cfg_transSliderTab", c);

        c = configKeeper.getConfiguratie("extent", appCode);
        writeString(dynaForm, "cfg_extent", c);

        c = configKeeper.getConfiguratie("fullextent", appCode);
        writeString(dynaForm, "cfg_fullextent", c);

        /* opslaan zoekinganen */
        writeZoekenConfig(dynaForm, appCode);
        writePlanSelectieIdConfig(dynaForm, appCode);

        writeMeldingConfig(dynaForm, appCode);

        c = configKeeper.getConfiguratie("redliningGegevensbron", appCode);
        writeInteger(dynaForm, "cfg_redlininggegevensbron", c);

        c = configKeeper.getConfiguratie("redliningkaartlaagid", appCode);
        writeInteger(dynaForm, "cfg_redliningkaartlaagid", c);

        // Teken settings
        c = configKeeper.getConfiguratie("tekenGegevensbron", appCode);
        writeInteger(dynaForm, "cfg_tekenGegevensbron", c);
        c = configKeeper.getConfiguratie("tekenTekstBoven", appCode);
        writeString(dynaForm, "cfg_tekenTekstBoven", c);
        c = configKeeper.getConfiguratie("tekenTekstOnder", appCode);
        writeString(dynaForm, "cfg_tekenTekstOnder", c);
        c = configKeeper.getConfiguratie("tekenTitel", appCode);
        writeString(dynaForm, "cfg_tekenTitel", c);
        c = configKeeper.getConfiguratie("tekenPlaatje", appCode);
        writeString(dynaForm, "cfg_tekenPlaatje", c);

        //BAG settings
        c = configKeeper.getConfiguratie("bagkaartlaagid", appCode);
        writeInteger(dynaForm, "cfg_bagkaartlaagid", c);
        //slider settings bag
        c = configKeeper.getConfiguratie("bagMaxBouwjaar", appCode);
        writeInteger(dynaForm, "cfg_bagmaxbouwjaar", c);
        c = configKeeper.getConfiguratie("bagMinBouwjaar", appCode);
        writeInteger(dynaForm, "cfg_bagminbouwjaar", c);
        c = configKeeper.getConfiguratie("bagMaxOpp", appCode);
        writeInteger(dynaForm, "cfg_bagmaxopp", c);
        c = configKeeper.getConfiguratie("bagMinOpp", appCode);
        writeInteger(dynaForm, "cfg_bagminopp", c);
        //attribute namen BAG
        c = configKeeper.getConfiguratie("bagOppAttr", appCode);
        writeString(dynaForm, "cfg_bagOppAttr", c);
        c = configKeeper.getConfiguratie("bagBouwjaarAttr", appCode);
        writeString(dynaForm, "cfg_bagBouwjaarAttr", c);
        c = configKeeper.getConfiguratie("bagGebruiksfunctieAttr", appCode);
        writeString(dynaForm, "cfg_bagGebruiksfunctieAttr", c);
        c = configKeeper.getConfiguratie("bagGeomAttr", appCode);
        writeString(dynaForm, "cfg_bagGeomAttr", c);
        c = configKeeper.getConfiguratie("useUserWmsDropdown", appCode);
        writeBoolean(dynaForm, "cfg_useUserWmsDropdown", c);
        c = configKeeper.getConfiguratie("datasetDownload", appCode);
        writeBoolean(dynaForm, "cfg_datasetDownload", c);
        c = configKeeper.getConfiguratie("tilingResolutions", appCode);
        writeString(dynaForm, "cfg_tilingResolutions", c);

        c = configKeeper.getConfiguratie("showServiceUrl", appCode);
        writeBoolean(dynaForm, "cfg_showServiceUrl", c);

        c = configKeeper.getConfiguratie("showInfoTab", appCode);
        writeString(dynaForm, "cfg_showInfoTab", c);

        c = configKeeper.getConfiguratie("helpUrl", appCode);
        writeString(dynaForm, "cfg_helpUrl", c);

        c = configKeeper.getConfiguratie("showGoogleMapsIcon", appCode);
        writeBoolean(dynaForm, "cfg_showGoogleMapsIcon", c);

        c = configKeeper.getConfiguratie("showBookmarkIcon", appCode);
        writeBoolean(dynaForm, "cfg_showBookmarkIcon", c);

        c = configKeeper.getConfiguratie("contactUrl", appCode);
        writeString(dynaForm, "cfg_contactUrl", c);

        c = configKeeper.getConfiguratie("logoutUrl", appCode);
        writeString(dynaForm, "cfg_logoutUrl", c);

        // opslaan teken tab sld filter settings
        c = configKeeper.getConfiguratie("tekenKaartlaagId", appCode);
        writeInteger(dynaForm, "cfg_tekenKaartlaagId", c);

        c = configKeeper.getConfiguratie("tekenFilterColumn", appCode);
        writeString(dynaForm, "cfg_tekenFilterColumn", c);

        c = configKeeper.getConfiguratie("tekenFilterSld", appCode);
        writeString(dynaForm, "cfg_tekenFilterSld", c);
    }

    private void writeMeldingConfig(DynaValidatorForm dynaForm, String appCode) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();

        Configuratie config = configKeeper.getConfiguratie("meldingWelkomtekst", appCode);
        writeString(dynaForm, "cfg_meldingwelkomtekst", config);

        config = configKeeper.getConfiguratie("meldingPrefix", appCode);
        writeString(dynaForm, "cfg_meldingprefix", config);

        config = configKeeper.getConfiguratie("meldingType", appCode);
        writeString(dynaForm, "cfg_meldingtype", config);

        config = configKeeper.getConfiguratie("meldingStatus", appCode);
        writeString(dynaForm, "cfg_meldingstatus", config);

        config = configKeeper.getConfiguratie("meldingLayoutEmailMelder", appCode);
        writeString(dynaForm, "cfg_meldinglayoutemailmelder", config);

        config = configKeeper.getConfiguratie("meldingNaam", appCode);
        writeString(dynaForm, "cfg_meldingnaam", config);

        config = configKeeper.getConfiguratie("meldingEmail", appCode);
        writeString(dynaForm, "cfg_meldingemail", config);

        config = configKeeper.getConfiguratie("meldingEmailMelder", appCode);
        writeBoolean(dynaForm, "cfg_meldingemailmelder", config);

        config = configKeeper.getConfiguratie("meldingEmailBehandelaar", appCode);
        writeBoolean(dynaForm, "cfg_meldingemailbehandelaar", config);

        config = configKeeper.getConfiguratie("meldingLayoutEmailBehandelaar", appCode);
        writeString(dynaForm, "cfg_meldinglayoutemailbehandelaar", config);

        config = configKeeper.getConfiguratie("meldingGegevensbron", appCode);
        writeInteger(dynaForm, "cfg_meldinggegevensbron", config);

        config = configKeeper.getConfiguratie("meldingObjectSoort", appCode);
        writeString(dynaForm, "cfg_meldingobjectsoort", config);

        config = configKeeper.getConfiguratie("meldingTekentoolIcoon", appCode);
        writeString(dynaForm, "cfg_meldingtekentoolicoon", config);

        config = configKeeper.getConfiguratie("smtpHost", appCode);
        writeString(dynaForm, "cfg_smtpHost", config);

        config = configKeeper.getConfiguratie("fromMailAddress", appCode);
        writeString(dynaForm, "cfg_fromMailAddress", config);

        config = configKeeper.getConfiguratie("mailSubject", appCode);
        writeString(dynaForm, "cfg_mailSubject", config);

        sess.flush();
    }

    private void writeZoekenConfig(DynaValidatorForm form, String appCode) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("zoekConfigIds", appCode);
        String strBeheerTabs = "";

        if (!form.get("cfg_zoekenid1").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid1") + ",";
        }

        if (!form.get("cfg_zoekenid2").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid2") + ",";
        }

        if (!form.get("cfg_zoekenid3").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid3") + ",";
        }

        if (!form.get("cfg_zoekenid4").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid4") + ",";
        }

        if (!form.get("cfg_zoekenid5").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid5") + ",";
        }

        if (!form.get("cfg_zoekenid6").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid6") + ",";
        }

        if (!form.get("cfg_zoekenid7").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid7") + ",";
        }

        if (!form.get("cfg_zoekenid8").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid8") + ",";
        }

        if (!form.get("cfg_zoekenid9").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid9") + ",";
        }

        if (!form.get("cfg_zoekenid10").equals("leeg")) {
            strBeheerTabs += form.get("cfg_zoekenid10") + ",";
        }

        lastComma = strBeheerTabs.lastIndexOf(",");

        if (lastComma > 0) {
            strBeheerTabs = strBeheerTabs.substring(0, lastComma);
        }

        strBeheerTabs = "\"" + strBeheerTabs + "\"";

        if (strBeheerTabs != null && strBeheerTabs.equals("\"\"")) {
            configTabs.setPropval(null);
        } else {
            configTabs.setPropval(strBeheerTabs);
        }

        configTabs.setType("java.lang.String");

        Configuratie config = configKeeper.getConfiguratie("zoekenAutoIdentify", appCode);
        writeBoolean(form, "cfg_zoeken_autoidentify", config);
        
        sess.merge(configTabs);
        sess.flush();
    }

    private void writePlanSelectieIdConfig(DynaValidatorForm form, String appCode) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("planSelectieIds", appCode);
        String strBeheerTabs = "";

        if (!form.get("cfg_planselectieid1").equals("leeg")) {
            strBeheerTabs += form.get("cfg_planselectieid1") + ",";
        }

        if (!form.get("cfg_planselectieid2").equals("leeg")) {
            strBeheerTabs += form.get("cfg_planselectieid2") + ",";
        }

        lastComma = strBeheerTabs.lastIndexOf(",");

        if (lastComma > 1) {
            strBeheerTabs = strBeheerTabs.substring(0, lastComma);
        }

        strBeheerTabs = "\"" + strBeheerTabs + "\"";

        configTabs.setPropval(strBeheerTabs);
        configTabs.setType("java.lang.String");
        sess.merge(configTabs);
        sess.flush();
    }

    private void writeTabbladenConfig(DynaValidatorForm form, String appCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("tabs", appCode);
        String strBeheerTabs = "";

        if (!form.get("cfg_tab1").equals("leeg")) {
            strBeheerTabs += "\"" + form.get("cfg_tab1") + "\",";
        }

        if (!form.get("cfg_tab2").equals("leeg")) {
            strBeheerTabs += "\"" + form.get("cfg_tab2") + "\",";
        }

        if (!form.get("cfg_tab3").equals("leeg")) {
            strBeheerTabs += "\"" + form.get("cfg_tab3") + "\",";
        }

        if (!form.get("cfg_tab4").equals("leeg")) {
            strBeheerTabs += "\"" + form.get("cfg_tab4") + "\",";
        }

        if (!form.get("cfg_tab5").equals("leeg")) {
            strBeheerTabs += "\"" + form.get("cfg_tab5") + "\",";
        }

        lastComma = strBeheerTabs.lastIndexOf(",");

        if (lastComma > 1) {
            strBeheerTabs = strBeheerTabs.substring(0, lastComma);
        }

        if (configTabs != null) {
            configTabs.setPropval(strBeheerTabs);
            configTabs.setType("java.lang.String");
            sess.merge(configTabs);
            sess.flush();
        }

        Configuratie configTabsLeft = configKeeper.getConfiguratie("tabsLeft", appCode);
        String strBeheerTabsLeft = "";

        if (!form.get("cfg_tab1_left").equals("leeg")) {
            strBeheerTabsLeft += "\"" + form.get("cfg_tab1_left") + "\",";
        }

        if (!form.get("cfg_tab2_left").equals("leeg")) {
            strBeheerTabsLeft += "\"" + form.get("cfg_tab2_left") + "\",";
        }

        if (!form.get("cfg_tab3_left").equals("leeg")) {
            strBeheerTabsLeft += "\"" + form.get("cfg_tab3_left") + "\",";
        }

        if (!form.get("cfg_tab4_left").equals("leeg")) {
            strBeheerTabsLeft += "\"" + form.get("cfg_tab4_left") + "\",";
        }

        if (!form.get("cfg_tab5_left").equals("leeg")) {
            strBeheerTabsLeft += "\"" + form.get("cfg_tab5_left") + "\",";
        }

        lastComma = strBeheerTabsLeft.lastIndexOf(",");

        if (lastComma > 1) {
            strBeheerTabsLeft = strBeheerTabsLeft.substring(0, lastComma);
        }

        if (configTabsLeft != null) {
            configTabsLeft.setPropval(strBeheerTabsLeft);
            configTabsLeft.setType("java.lang.String");
            sess.merge(configTabsLeft);
            sess.flush();
        }
    }

    private void writeBoolean(DynaValidatorForm form, String field, Configuratie c) {
        if (c != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

            if (form.get(field) != null && form.get(field).toString().length() > 0) {
                c.setPropval("true");
            } else {
                c.setPropval("false");
            }
            c.setType("java.lang.Boolean");

            sess.merge(c);
            sess.flush();
        }
    }

    private void writeInteger(DynaValidatorForm form, String field, Configuratie c) {
        if (c != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

            if (form.get(field) != null) {
                c.setPropval(form.get(field).toString());
            } else {
                c.setPropval("0");
            }
            c.setType("java.lang.Integer");

            sess.merge(c);
            sess.flush();
        }
    }

    private void writeString(DynaValidatorForm form, String field, Configuratie c) {
        if (c != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

            if (form.get(field) != null) {
                c.setPropval(form.get(field).toString());
            } else {
                c.setPropval("");
            }
            c.setType("java.lang.String");

            sess.merge(c);
            sess.flush();
        }
    }

    private void fillTabbladenConfig(DynaValidatorForm dynaForm, Map map) {
        String tabs = (String) map.get("tabs");
        if (tabs == null) {
            return;
        }
        String[] items = tabs.split(",");

        if (items.length > 0) {
            if ((items[0] != null) || items[0].equals("")) {
                String cfg_tab1 = items[0].replaceAll("\"", "");
                dynaForm.set("cfg_tab1", cfg_tab1);
            }
        } else {
            dynaForm.set("cfg_tab1", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 1) {
            if ((items[1] != null) || items[1].equals("")) {
                String cfg_tab2 = items[1].replaceAll("\"", "");
                dynaForm.set("cfg_tab2", cfg_tab2);
            }
        } else {
            dynaForm.set("cfg_tab2", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 2) {
            if ((items[2] != null) || items[2].equals("")) {
                String cfg_tab3 = items[2].replaceAll("\"", "");
                dynaForm.set("cfg_tab3", cfg_tab3);
            }
        } else {
            dynaForm.set("cfg_tab3", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 3) {
            if ((items[3] != null) || items[3].equals("")) {
                String cfg_tab4 = items[3].replaceAll("\"", "");
                dynaForm.set("cfg_tab4", cfg_tab4);
            }
        } else {
            dynaForm.set("cfg_tab4", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 4) {
            if ((items[4] != null) || items[4].equals("")) {
                String cfg_tab5 = items[4].replaceAll("\"", "");
                dynaForm.set("cfg_tab5", cfg_tab5);
            }
        } else {
            dynaForm.set("cfg_tab5", CONFIGKEEPER_TABS[0]);
        }

        // fill tab settings left
        tabs = (String) map.get("tabsLeft");
        if (tabs == null) {
            return;
        }
        items = tabs.split(",");

        if (items.length > 0) {
            if ((items[0] != null) || items[0].equals("")) {
                String cfg_tab1 = items[0].replaceAll("\"", "");
                dynaForm.set("cfg_tab1_left", cfg_tab1);
            }
        } else {
            dynaForm.set("cfg_tab1_left", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 1) {
            if ((items[1] != null) || items[1].equals("")) {
                String cfg_tab2 = items[1].replaceAll("\"", "");
                dynaForm.set("cfg_tab2_left", cfg_tab2);
            }
        } else {
            dynaForm.set("cfg_tab2_left", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 2) {
            if ((items[2] != null) || items[2].equals("")) {
                String cfg_tab3 = items[2].replaceAll("\"", "");
                dynaForm.set("cfg_tab3_left", cfg_tab3);
            }
        } else {
            dynaForm.set("cfg_tab3_left", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 3) {
            if ((items[3] != null) || items[3].equals("")) {
                String cfg_tab4 = items[3].replaceAll("\"", "");
                dynaForm.set("cfg_tab4_left", cfg_tab4);
            }
        } else {
            dynaForm.set("cfg_tab4_left", CONFIGKEEPER_TABS[0]);
        }

        if (items.length > 4) {
            if ((items[4] != null) || items[4].equals("")) {
                String cfg_tab5 = items[4].replaceAll("\"", "");
                dynaForm.set("cfg_tab5_left", cfg_tab5);
            }
        } else {
            dynaForm.set("cfg_tab5_left", CONFIGKEEPER_TABS[0]);
        }
    }

    private void fillZoekConfigBox(DynaValidatorForm dynaForm,
            HttpServletRequest request, Map map) {

        // Auto identify is true by default
        Boolean autoIdentify = true;
        if(map.containsKey("zoekenAutoIdentify")) {
            // If the auto identify value is set, use this value
            autoIdentify = (Boolean) map.get("zoekenAutoIdentify");
        }
        dynaForm.set("cfg_zoeken_autoidentify", autoIdentify);
        
        String ids = (String) map.get("zoekConfigIds");
        
        if (ids == null) {
            return;
        }

        String[] items = ids.replaceAll("\"", "").split(",");

        if (items.length > 0) {
            if ((items[0] != null) || items[0].equals("")) {
                dynaForm.set("cfg_zoekenid1", items[0].replaceAll("\"", ""));
            }
        }

        if (items.length > 1) {
            if ((items[1] != null) || items[1].equals("")) {
                dynaForm.set("cfg_zoekenid2", items[1].replaceAll("\"", ""));
            }
        }

        if (items.length > 2) {
            if ((items[2] != null) || items[2].equals("")) {
                dynaForm.set("cfg_zoekenid3", items[2].replaceAll("\"", ""));
            }
        }

        if (items.length > 3) {
            if ((items[3] != null) || items[3].equals("")) {
                dynaForm.set("cfg_zoekenid4", items[3].replaceAll("\"", ""));
            }
        }

        if (items.length > 4) {
            if ((items[4] != null) || items[4].equals("")) {
                dynaForm.set("cfg_zoekenid5", items[4].replaceAll("\"", ""));
            }
        }

        if (items.length > 5) {
            if ((items[5] != null) || items[5].equals("")) {
                dynaForm.set("cfg_zoekenid6", items[5].replaceAll("\"", ""));
            }
        }

        if (items.length > 6) {
            if ((items[6] != null) || items[6].equals("")) {
                dynaForm.set("cfg_zoekenid7", items[6].replaceAll("\"", ""));
            }
        }

        if (items.length > 7) {
            if ((items[7] != null) || items[7].equals("")) {
                dynaForm.set("cfg_zoekenid8", items[7].replaceAll("\"", ""));
            }
        }

        if (items.length > 8) {
            if ((items[8] != null) || items[8].equals("")) {
                dynaForm.set("cfg_zoekenid9", items[8].replaceAll("\"", ""));
            }
        }

        if (items.length > 9) {
            if ((items[9] != null) || items[9].equals("")) {
                dynaForm.set("cfg_zoekenid10", items[9].replaceAll("\"", ""));
            }
        }
    }

    private void fillPlanSelectieBox(DynaValidatorForm dynaForm,
            HttpServletRequest request, String ids) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();

        request.setAttribute("zoekConfigs", zoekconfigs);

        if (ids == null) {
            return;
        }

        String[] items = ids.replaceAll("\"", "").split(",");

        if (items.length > 0) {
            if ((items[0] != null) || items[0].equals("")) {
                dynaForm.set("cfg_planselectieid1", items[0].replaceAll("\"", ""));
            }
        }

        if (items.length > 1) {
            if ((items[1] != null) || items[1].equals("")) {
                dynaForm.set("cfg_planselectieid2", items[1].replaceAll("\"", ""));
            }
        }
    }

    private void fillMeldingenBox(DynaValidatorForm dynaForm,
            HttpServletRequest request, Map map) {

        dynaForm.set("cfg_meldingwelkomtekst", (String) map.get("meldingWelkomtekst"));
        dynaForm.set("cfg_meldingprefix", (String) map.get("meldingPrefix"));
        dynaForm.set("cfg_meldingtype", (String) map.get("meldingType"));
        dynaForm.set("cfg_meldingstatus", (String) map.get("meldingStatus"));
        dynaForm.set("cfg_meldingemailmelder", (Boolean) map.get("meldingEmailMelder"));
        dynaForm.set("cfg_meldinglayoutemailmelder", (String) map.get("meldingLayoutEmailMelder"));
        dynaForm.set("cfg_meldingnaam", (String) map.get("meldingNaam"));
        dynaForm.set("cfg_meldingemail", (String) map.get("meldingEmail"));
        dynaForm.set("cfg_meldingemailbehandelaar", (Boolean) map.get("meldingEmailBehandelaar"));
        dynaForm.set("cfg_meldinglayoutemailbehandelaar", (String) map.get("meldingLayoutEmailBehandelaar"));
        dynaForm.set("cfg_meldinggegevensbron", (Integer) map.get("meldingGegevensbron"));
        dynaForm.set("cfg_meldingobjectsoort", (String) map.get("meldingObjectSoort"));
        dynaForm.set("cfg_meldingtekentoolicoon", (String) map.get("meldingTekentoolIcoon"));
        dynaForm.set("cfg_smtpHost", (String) map.get("smtpHost"));
        dynaForm.set("cfg_fromMailAddress", (String) map.get("fromMailAddress"));
        dynaForm.set("cfg_mailSubject", (String) map.get("mailSubject"));
    }
}
