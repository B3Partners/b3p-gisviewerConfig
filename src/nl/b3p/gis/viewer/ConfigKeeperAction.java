package nl.b3p.gis.viewer;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.json.JSONException;

public class ConfigKeeperAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigKeeperAction.class);
    private static final String[] CONFIGKEEPER_TABS = {
        "leeg", "themas", "legenda", "zoeken", "informatie", "gebieden",
        "analyse", "planselectie", "meldingen", "vergunningen", "voorzieningen",
        "redlining","cms","bag"
    };
    private static final String[] LABELS_VOOR_TABS = {
        "-Kies een tabblad-", "Kaarten", "Legenda", "Zoeken", "Info", "Gebieden",
        "Analyse", "Plannen", "Meldingen", "Vergunningen", "Voorzieningen",
        "Redlining","CMS","BAG"
    };
    
    protected static final String RESET_INSTELLINGEN = "resetInstellingen";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(RESET_INSTELLINGEN);
        crudProp.setDefaultForwardName(SUCCESS);
        map.put(RESET_INSTELLINGEN, crudProp);

        return map;
    }

    public ActionForward resetInstellingen(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return this.getAlternateForward(mapping, request);
        }

        String appCode = dynaForm.getString("appcode");        

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        int query = sess.createQuery("delete from Configuratie where setting = :appcode)")
                .setParameter("appcode", appCode)
                .executeUpdate();

        sess.flush();

        if (query > 0) {
            writeDefaultApplicatie(appCode);
            logger.debug("Rolinstellingen zijn gereset voor appcode " + appCode);
        }

        ConfigKeeper configKeeper = new ConfigKeeper();

        Map map = null;
        map = configKeeper.getConfigMap(appCode);

        if (map.size() > 1) {
            populateForm(dynaForm, request, map, appCode);
        }

        KaartSelectieUtil.removeExistingUserKaartgroepAndUserKaartlagen(appCode);
        KaartSelectieUtil.resetExistingUserLayers(appCode);

        /* Basisboom ophalen */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        request.setAttribute("header_appcode", appCode);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* Applicatie code ophalen */
        String appCode = (String) request.getParameter("appcode");

        /* Basisboom ophalen */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);

        Map map = null;

        ConfigKeeper configKeeper = new ConfigKeeper();
        map = configKeeper.getConfigMap(appCode);

        /* TODO weer uncommenten */
        if (map.size() < 1) {
            writeDefaultApplicatie(appCode);
            map = configKeeper.getConfigMap(appCode);
        }

        populateForm(dynaForm, request, map, appCode);

        request.setAttribute("header_appcode", appCode);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        request.setAttribute("tabValues", CONFIGKEEPER_TABS);
        request.setAttribute("tabLabels", LABELS_VOOR_TABS);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();
        request.setAttribute("zoekConfigs", zoekconfigs);

        List meldingGegevensbronnen = sess.createQuery("from Gegevensbron order by naam").list();
        request.setAttribute("meldingGegevensbronnen", meldingGegevensbronnen);

        List redliningKaartlagen = sess.createQuery("from Themas order by naam").list();
        request.setAttribute("redliningKaartlagen", redliningKaartlagen);

        /* klaarzetten wms layers voor keuze opstartlagen */
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        List lns = user.getLayers(false, true);
        request.setAttribute("listLayers", lns);
    }

    public void populateForm(DynaValidatorForm dynaForm, HttpServletRequest request, Map map, String appCode) {

        Boolean useCookies = (Boolean) map.get("useCookies");
        Boolean usePopup = (Boolean) map.get("usePopup");
        Boolean useDivPopup = (Boolean) map.get("useDivPopup");
        Boolean usePanelControls = (Boolean) map.get("usePanelControls");
        Integer autoRedirect = (Integer) map.get("autoRedirect");
        Integer tolerance = (Integer) map.get("tolerance");
        Integer refreshDelay = (Integer) map.get("refreshDelay");
        String zoekConfigIds = (String) map.get("zoekConfigIds");
        String planSelectieIds = (String) map.get("planSelectieIds");
        Integer minBboxZoeken = (Integer) map.get("minBboxZoeken");
        Integer maxResults = (Integer) map.get("maxResults");
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

        /* vullen box voor zoek ingangen */
        fillZoekConfigBox(dynaForm, request, zoekConfigIds);
        fillPlanSelectieBox(dynaForm, request, planSelectieIds);

        String voorzieningConfigIds = (String) map.get("voorzieningConfigIds");
        String voorzieningConfigStraal = (String) map.get("voorzieningConfigStraal");
        String voorzieningConfigTypes = (String) map.get("voorzieningConfigTypes");
        fillVoorzieningConfigBox(dynaForm, request, voorzieningConfigIds, voorzieningConfigStraal, voorzieningConfigTypes);

        String vergunningConfigIds = (String) map.get("vergunningConfigIds");
        String vergunningConfigStraal = (String) map.get("vergunningConfigStraal");
        fillVergunningConfigBox(dynaForm, request, vergunningConfigIds, vergunningConfigStraal);

        fillMeldingenBox(dynaForm, request, map);

        /* overige settings klaarzetten voor formulier */
        dynaForm.set("cfg_useCookies", useCookies);
        dynaForm.set("cfg_autoRedirect", autoRedirect);
        dynaForm.set("cfg_tolerance", tolerance);
        dynaForm.set("cfg_refreshDelay", refreshDelay);
        dynaForm.set("cfg_minBboxZoeken", minBboxZoeken);
        dynaForm.set("cfg_maxResults", maxResults);
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

        dynaForm.set("appcode", appCode);

        /* Tabbladen vullen */
        fillTabbladenConfig(dynaForm, map);

        /* redlining config items */
        dynaForm.set("cfg_redlininggegevensbron", (Integer) map.get("redliningGegevensbron"));
        dynaForm.set("cfg_redliningkaartlaagid", (Integer) map.get("redliningkaartlaagid"));

        /* Bag config items*/
        if (map.get("bagkaartlaagid")!=null)
            dynaForm.set("cfg_bagkaartlaagid", (Integer) map.get("bagkaartlaagid"));
        if (map.get("bagMaxBouwjaar")!=null)
            dynaForm.set("cfg_bagmaxbouwjaar", (Integer) map.get("bagMaxBouwjaar"));
        if (map.get("bagMinBouwjaar")!=null)
            dynaForm.set("cfg_bagminbouwjaar", (Integer) map.get("bagMinBouwjaar"));
        if (map.get("bagMaxOpp")!=null)
            dynaForm.set("cfg_bagmaxopp", (Integer) map.get("bagMaxOpp"));
        if (map.get("bagMinOpp")!=null)
            dynaForm.set("cfg_bagminopp", (Integer) map.get("bagMinOpp"));
        if (map.get("bagOppAttr")!=null)
            dynaForm.set("cfg_bagOppAttr", (String)map.get("bagOppAttr"));
        if (map.get("bagBouwjaarAttr")!=null)
            dynaForm.set("cfg_bagBouwjaarAttr", (String) map.get("bagBouwjaarAttr"));
        if (map.get("bagGebruiksfunctieAttr")!=null)
            dynaForm.set("cfg_bagGebruiksfunctieAttr", (String) map.get("bagGebruiksfunctieAttr"));
        if (map.get("bagGeomAttr")!=null)
            dynaForm.set("cfg_bagGeomAttr", (String) map.get("bagGeomAttr"));
        
        
        /* klaarzetten wms layers voor keuze opstartlagen */
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);

        List lns = user.getLayers(false, true);
        request.setAttribute("listLayers", lns);

        /* opstartlagen klaarzetten */
        String opstartKaarten = (String) map.get("opstartKaarten");

        if (opstartKaarten != null) {
            dynaForm.set("cfg_opstartkaarten", opstartKaarten.split(","));
        }
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

        request.setAttribute("header_appcode", appCode);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        
        return getDefaultForward(mapping, request);
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

        /* Basisboom ophalen */
        KaartSelectieUtil.populateKaartSelectieForm(appCode, request);
    }

    public void populateObject(DynaValidatorForm dynaForm, String appCode) {

        /* opslaan overige settings */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie c = null;

        /* opslaan opstartkaarten */
        writeOpstartKaartenConfig(dynaForm, appCode);

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

        /* opslaan zoekinganen */
        writeZoekenIdConfig(dynaForm, appCode);
        writePlanSelectieIdConfig(dynaForm, appCode);
        writeVoorzieningIdConfig(dynaForm, appCode);
        writeVergunningIdConfig(dynaForm, appCode);

        writeMeldingConfig(dynaForm, appCode);

        c = configKeeper.getConfiguratie("redliningGegevensbron", appCode);
        writeInteger(dynaForm, "cfg_redlininggegevensbron", c);

        c = configKeeper.getConfiguratie("redliningkaartlaagid", appCode);
        writeInteger(dynaForm, "cfg_redliningkaartlaagid", c);
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
        writeString(dynaForm,"cfg_bagOppAttr",c);
        c = configKeeper.getConfiguratie("bagBouwjaarAttr", appCode);
        writeString(dynaForm,"cfg_bagBouwjaarAttr",c);
        c = configKeeper.getConfiguratie("bagGebruiksfunctieAttr", appCode);
        writeString(dynaForm,"cfg_bagGebruiksfunctieAttr",c);
        c = configKeeper.getConfiguratie("bagGeomAttr", appCode);
        writeString(dynaForm,"cfg_bagGeomAttr",c);
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

        config = configKeeper.getConfiguratie("meldingEmailmelder", appCode);
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

    private void writeZoekenIdConfig(DynaValidatorForm form, String appCode) {

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

    private void writeVoorzieningIdConfig(DynaValidatorForm form, String appCode) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("voorzieningConfigIds", appCode);
        String strBeheerTabs = "";

        if (!form.get("cfg_voorzieningid1").equals("leeg")) {
            strBeheerTabs += form.get("cfg_voorzieningid1") + ",";
        }

        if (!form.get("cfg_voorzieningid2").equals("leeg")) {
            strBeheerTabs += form.get("cfg_voorzieningid2") + ",";
        }

        if (!form.get("cfg_voorzieningid3").equals("leeg")) {
            strBeheerTabs += form.get("cfg_voorzieningid3") + ",";
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


        Configuratie configStraal = configKeeper.getConfiguratie("voorzieningConfigStraal", appCode);
        String straal = "";
        if (!form.get("cfg_voorzieningstraal").equals("leeg")) {
            straal = form.get("cfg_voorzieningstraal") + "";
        }
        straal = "\"" + straal + "\"";

        configStraal.setPropval(straal);
        configStraal.setType("java.lang.String");
        sess.merge(configStraal);
        sess.flush();


        int lastTypComma = -1;

        Configuratie configType = configKeeper.getConfiguratie("voorzieningConfigTypes", appCode);
        String strTypeTabs = "";

        if (!form.get("cfg_voorzieningtype1").equals("leeg")) {
            strTypeTabs += form.get("cfg_voorzieningtype1") + ",";
        }

        if (!form.get("cfg_voorzieningtype2").equals("leeg")) {
            strTypeTabs += form.get("cfg_voorzieningtype2") + ",";
        }

        if (!form.get("cfg_voorzieningtype3").equals("leeg")) {
            strTypeTabs += form.get("cfg_voorzieningtype3") + ",";
        }

        if (!form.get("cfg_voorzieningtype4").equals("leeg")) {
            strTypeTabs += form.get("cfg_voorzieningtype4") + ",";
        }

        if (!form.get("cfg_voorzieningtype5").equals("leeg")) {
            strTypeTabs += form.get("cfg_voorzieningtype5") + ",";
        }

        if (!form.get("cfg_voorzieningtype6").equals("leeg")) {
            strTypeTabs += form.get("cfg_voorzieningtype6") + ",";
        }

        lastTypComma = strTypeTabs.lastIndexOf(",");

        if (lastComma > 1) {
            strTypeTabs = strTypeTabs.substring(0, lastTypComma);
        }

        strTypeTabs = "\"" + strTypeTabs + "\"";

        configType.setPropval(strTypeTabs);
        configType.setType("java.lang.String");
        sess.merge(configType);
        sess.flush();
    }

    private void writeVergunningIdConfig(DynaValidatorForm form, String appCode) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("vergunningConfigIds", appCode);
        String strBeheerTabs = "";

        if (!form.get("cfg_vergunningid1").equals("leeg")) {
            strBeheerTabs += form.get("cfg_vergunningid1") + ",";
        }

        if (!form.get("cfg_vergunningid2").equals("leeg")) {
            strBeheerTabs += form.get("cfg_vergunningid2") + ",";
        }

        if (!form.get("cfg_vergunningid3").equals("leeg")) {
            strBeheerTabs += form.get("cfg_vergunningid3") + ",";
        }

        if (!form.get("cfg_vergunningid4").equals("leeg")) {
            strBeheerTabs += form.get("cfg_vergunningid4") + ",";
        }

        if (!form.get("cfg_vergunningid5").equals("leeg")) {
            strBeheerTabs += form.get("cfg_vergunningid5") + ",";
        }

        if (!form.get("cfg_vergunningid6").equals("leeg")) {
            strBeheerTabs += form.get("cfg_vergunningid6") + ",";
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


        Configuratie configStraal = configKeeper.getConfiguratie("vergunningConfigStraal", appCode);
        String straal = "";
        if (!form.get("cfg_vergunningstraal").equals("leeg")) {
            straal = form.get("cfg_vergunningstraal") + "";
        }
        straal = "\"" + straal + "\"";

        configStraal.setPropval(straal);
        configStraal.setType("java.lang.String");
        sess.merge(configStraal);
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

    private void writeOpstartKaartenConfig(DynaValidatorForm form, String appCode) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie opstartKaarten = configKeeper.getConfiguratie("opstartKaarten", appCode);

        String strKaarten = "";
        String[] arrKaarten = (String[]) form.get("cfg_opstartkaarten");

        if (arrKaarten != null && arrKaarten.length > 0) {
            for (int i=0; i < arrKaarten.length; i++) {
                strKaarten += arrKaarten[i] + ",";
            }

            lastComma = strKaarten.lastIndexOf(",");

            if (lastComma > 1) {
                strKaarten = strKaarten.substring(0, lastComma);
            }

            opstartKaarten.setPropval(strKaarten);
            opstartKaarten.setType("java.lang.String");
            sess.merge(opstartKaarten);
            sess.flush();
        }

        if (arrKaarten != null && arrKaarten.length == 0) {
            opstartKaarten.setPropval(null);
            opstartKaarten.setType("java.lang.String");
            sess.merge(opstartKaarten);
            sess.flush();
        }
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
            strBeheerTabs += "\"" + form.get("cfg_tab5")+ "\",";
        }

        lastComma = strBeheerTabs.lastIndexOf(",");

        if (lastComma > 1) {
            strBeheerTabs = strBeheerTabs.substring(0, lastComma);
        }

        configTabs.setPropval(strBeheerTabs);
        configTabs.setType("java.lang.String");
        sess.merge(configTabs);
        sess.flush();
    }

    private void writeBoolean(DynaValidatorForm form, String field, Configuratie c) {
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

    private void writeInteger(DynaValidatorForm form, String field, Configuratie c) {
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

    private void writeString(DynaValidatorForm form, String field, Configuratie c) {
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
    }

    private void fillZoekConfigBox(DynaValidatorForm dynaForm,
            HttpServletRequest request, String ids) {

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

    private void fillVoorzieningConfigBox(DynaValidatorForm dynaForm,
            HttpServletRequest request, String ids, String voorzieningConfigStraal, String voorzieningConfigTypes) {

        if (ids != null) {
            String[] items = ids.replaceAll("\"", "").split(",");

            if (items.length > 0) {
                if ((items[0] != null) || items[0].equals("")) {
                    dynaForm.set("cfg_voorzieningid1", items[0].replaceAll("\"", ""));
                }
            }

            if (items.length > 1) {
                if ((items[1] != null) || items[1].equals("")) {
                    dynaForm.set("cfg_voorzieningid2", items[1].replaceAll("\"", ""));
                }
            }

            if (items.length > 2) {
                if ((items[2] != null) || items[2].equals("")) {
                    dynaForm.set("cfg_voorzieningid3", items[2].replaceAll("\"", ""));
                }
            }
        }

        if (voorzieningConfigStraal != null) {
            dynaForm.set("cfg_voorzieningstraal", voorzieningConfigStraal.replaceAll("\"", ""));
        }

        if (voorzieningConfigTypes != null) {
            String[] typeItems = voorzieningConfigTypes.replaceAll("\"", "").split(",");

            if (typeItems.length > 0) {
                if ((typeItems[0] != null) || typeItems[0].equals("")) {
                    dynaForm.set("cfg_voorzieningtype1", typeItems[0].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 1) {
                if ((typeItems[1] != null) || typeItems[1].equals("")) {
                    dynaForm.set("cfg_voorzieningtype2", typeItems[1].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 2) {
                if ((typeItems[2] != null) || typeItems[2].equals("")) {
                    dynaForm.set("cfg_voorzieningtype3", typeItems[2].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 3) {
                if ((typeItems[3] != null) || typeItems[3].equals("")) {
                    dynaForm.set("cfg_voorzieningtype4", typeItems[3].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 4) {
                if ((typeItems[4] != null) || typeItems[4].equals("")) {
                    dynaForm.set("cfg_voorzieningtype5", typeItems[4].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 5) {
                if ((typeItems[5] != null) || typeItems[5].equals("")) {
                    dynaForm.set("cfg_voorzieningtype6", typeItems[5].replaceAll("\"", ""));
                }
            }
        }

    }

    private void fillVergunningConfigBox(DynaValidatorForm dynaForm,
            HttpServletRequest request, String ids, String vergunningConfigStraal) {

        if (ids != null) {
            String[] items = ids.replaceAll("\"", "").split(",");

            if (items.length > 0) {
                if ((items[0] != null) || items[0].equals("")) {
                    dynaForm.set("cfg_vergunningid1", items[0].replaceAll("\"", ""));
                }
            }

            if (items.length > 1) {
                if ((items[1] != null) || items[1].equals("")) {
                    dynaForm.set("cfg_vergunningid2", items[1].replaceAll("\"", ""));
                }
            }

            if (items.length > 2) {
                if ((items[2] != null) || items[2].equals("")) {
                    dynaForm.set("cfg_vergunningid3", items[2].replaceAll("\"", ""));
                }
            }

            if (items.length > 3) {
                if ((items[3] != null) || items[3].equals("")) {
                    dynaForm.set("cfg_vergunningid4", items[3].replaceAll("\"", ""));
                }
            }

            if (items.length > 4) {
                if ((items[4] != null) || items[4].equals("")) {
                    dynaForm.set("cfg_vergunningid5", items[4].replaceAll("\"", ""));
                }
            }

            if (items.length > 5) {
                if ((items[5] != null) || items[5].equals("")) {
                    dynaForm.set("cfg_vergunningid6", items[5].replaceAll("\"", ""));
                }
            }
        }

        if (vergunningConfigStraal != null) {
            dynaForm.set("cfg_vergunningstraal", vergunningConfigStraal.replaceAll("\"", ""));
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

    private void writeDefaultApplicatie(String appCode) {
        Configuratie cfg = null;

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        cfg = new Configuratie();
        cfg.setProperty("useCookies");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("multipleActiveThemas");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("dataframepopupHandle");
        cfg.setPropval("null");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLeftPanel");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("autoRedirect");
        cfg.setPropval("2");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useSortableFunction");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layerDelay");
        cfg.setPropval("5000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("refreshDelay");
        cfg.setPropval("1000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("minBboxZoeken");
        cfg.setPropval("1000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("zoekConfigIds");
        cfg.setPropval("\"-1\"");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("maxResults");
        cfg.setPropval("25");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("usePopup");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useDivPopup");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("usePanelControls");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("expandAll");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tolerance");
        cfg.setPropval("4");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useInheritCheckbox");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLegendInTree");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useMouseOverTabs");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layoutAdminData");
        cfg.setPropval("admindata1");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tabs");
        cfg.setPropval("\"themas\",\"legenda\",\"zoeken\"");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("planSelectieIds");
        cfg.setPropval("-1");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showRedliningTools");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showBufferTool");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showSelectBulkTool");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showNeedleTool");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showPrintTool");
        cfg.setPropval("true");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLayerSelectionTool");
        cfg.setPropval("false");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layerGrouping");
        cfg.setPropval("lg_cluster");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupWidth");
        cfg.setPropval("90%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupHeight");
        cfg.setPropval("20%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupLeft");
        cfg.setPropval("5%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupTop");
        cfg.setPropval("75%");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("defaultdataframehoogte");
        cfg.setPropval("150");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("viewerType");
        cfg.setPropval("flamingo");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("viewerTemplate");
        cfg.setPropval("standalone");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("objectInfoType");
        cfg.setPropval("popup");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("treeOrder");
        cfg.setPropval("volgorde");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tabWidth");
        cfg.setPropval("288");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);
        //BAG
        cfg = new Configuratie();
        cfg.setProperty("bagMaxBouwjaar");
        cfg.setPropval(""+Calendar.getInstance().get(Calendar.YEAR));
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagMinBouwjaar");
        cfg.setPropval("1000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagMaxOpp");
        cfg.setPropval("16000");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagMinOpp");
        cfg.setPropval("0");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagOppAttr");
        cfg.setPropval("OPPERVLAKTE");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagBouwjaarAttr");
        cfg.setPropval("BOUWJAAR");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagGebruiksfunctieAttr");
        cfg.setPropval("GEBRUIKSFUNCTIE");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("bagGeomAttr");
        cfg.setPropval("the_geom");
        cfg.setSetting(appCode);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        sess.flush();
    }
}
