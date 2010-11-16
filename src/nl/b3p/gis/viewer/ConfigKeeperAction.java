package nl.b3p.gis.viewer;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class ConfigKeeperAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigKeeperAction.class);
    private static final String[] CONFIGKEEPER_TABS = {
        "leeg", "themas", "legenda", "zoeken", "informatie", "gebieden",
        "analyse", "planselectie", "meldingen", "vergunningen", "voorzieningen"
    };
    private static final String[] LABELS_VOOR_TABS = {
        "-Kies een tabblad-", "Kaarten", "Legenda", "Zoeken", "Info", "Gebieden",
        "Analyse", "Plannen", "Meldingen", "Vergunningen", "Voorzieningen"
    };

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* rol ophalen */
        String rolnaam = (String) request.getParameter("rolnaam");

        Map map = null;

        ConfigKeeper configKeeper = new ConfigKeeper();
        map = configKeeper.getConfigMap(rolnaam);

        /* TODO weer uncommenten */
        if (map.size() < 1) {
            writeDefaultConfigForRole(rolnaam);
            map = configKeeper.getConfigMap(rolnaam);
        }

        populateForm(dynaForm, request, map, rolnaam);

        request.setAttribute("header_Rolnaam", rolnaam);
 
        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        request.setAttribute("tabValues", CONFIGKEEPER_TABS);
        request.setAttribute("tabLabels", LABELS_VOOR_TABS);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();
        request.setAttribute("zoekConfigs", zoekconfigs);

    }

    public void populateForm(DynaValidatorForm dynaForm, HttpServletRequest request, Map map, String rolnaam) {

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
        String layerGrouping = (String) map.get("layerGrouping");
        String popupWidth = (String) map.get("popupWidth");
        String popupHeight = (String) map.get("popupHeight");
        String popupLeft = (String) map.get("popupLeft");
        String popupTop = (String) map.get("popupTop");
        String defaultdataframehoogte = (String) map.get("defaultdataframehoogte");

        /* vullen box voor zoek ingangen */
        fillZoekConfigBox(dynaForm, request, zoekConfigIds);
        fillPlanSelectieBox(dynaForm, request, planSelectieIds);

        String voorzieningConfigIds = (String) map.get("voorzieningConfigIds");
        String voorzieningConfigStraal = (String) map.get("voorzieningConfigStraal");
        String voorzieningConfigTypes = (String) map.get("voorzieningConfigTypes");
        fillVoorzieningConfigBox(dynaForm, request, voorzieningConfigIds, voorzieningConfigStraal, voorzieningConfigTypes);

        String vergunningConfigIds = (String) map.get("vergunningConfigIds");
        String vergunningConfigStraal = (String) map.get("vergunningConfigStraal");
        String vergunningConfigTypes = (String) map.get("vergunningConfigTypes");
        String vergunningConfigVeld = (String) map.get("vergunningConfigVeld");
        String vergunningConfigTerm = (String) map.get("vergunningConfigTerm");
        fillVergunningConfigBox(dynaForm, request, vergunningConfigIds, vergunningConfigStraal, vergunningConfigTypes, vergunningConfigVeld, vergunningConfigTerm);

         /* dropdown voor i-tool goedzetten
        geen, paneel of popup */
        if (usePopup != null && usePopup) {
            dynaForm.set("cfg_objectInfo", "popup");
        } else {
            if (usePanelControls != null && usePanelControls) {
                dynaForm.set("cfg_objectInfo", "paneel");
            } else {
                dynaForm.set("cfg_objectInfo", "geen");
            }
        }
        
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
        dynaForm.set("cfg_layerGrouping", layerGrouping);
        dynaForm.set("cfg_popupWidth", popupWidth);
        dynaForm.set("cfg_popupHeight", popupHeight);
        dynaForm.set("cfg_popupLeft", popupLeft);
        dynaForm.set("cfg_popupTop", popupTop);
        dynaForm.set("cfg_defaultdataframehoogte", defaultdataframehoogte);

        dynaForm.set("rolnaam", rolnaam);
        
        /* Tabbladen vullen */
        fillTabbladenConfig(dynaForm, map);

    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String rolnaam = (String) dynaForm.get("rolnaam");

        if (!isTokenValid(request)) {
            prepareMethod(dynaForm, request, EDIT, LIST);
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return this.getAlternateForward(mapping, request);
        }

        populateObject(dynaForm, rolnaam);
        
        request.setAttribute("header_Rolnaam", rolnaam);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);
        return getDefaultForward(mapping, request);
    }

    public void populateObject(DynaValidatorForm dynaForm, String rolnaam) {
       /* opslaan I-tool dropdown */
        writeObjectInfoDisplayMethod(dynaForm, rolnaam);

        /* opslaan overige settings */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie c = null;

        /* Opslaan tabbladen */
        writeTabbladenConfig(dynaForm, rolnaam);

        c = configKeeper.getConfiguratie("useCookies", rolnaam);
        writeBoolean(dynaForm, "cfg_useCookies", c);

        c = configKeeper.getConfiguratie("autoRedirect", rolnaam);
        writeInteger(dynaForm, "cfg_autoRedirect", c);

        c = configKeeper.getConfiguratie("tolerance", rolnaam);
        writeInteger(dynaForm, "cfg_tolerance", c);

        c = configKeeper.getConfiguratie("refreshDelay", rolnaam);
        writeInteger(dynaForm, "cfg_refreshDelay", c);

        c = configKeeper.getConfiguratie("minBboxZoeken", rolnaam);
        writeInteger(dynaForm, "cfg_minBboxZoeken", c);

        c = configKeeper.getConfiguratie("maxResults", rolnaam);
        writeInteger(dynaForm, "cfg_maxResults", c);

        c = configKeeper.getConfiguratie("expandAll", rolnaam);
        writeBoolean(dynaForm, "cfg_expandAll", c);

        c = configKeeper.getConfiguratie("multipleActiveThemas", rolnaam);
        writeBoolean(dynaForm, "cfg_multipleActiveThemas", c);

        c = configKeeper.getConfiguratie("useInheritCheckbox", rolnaam);
        writeBoolean(dynaForm, "cfg_useInheritCheckbox", c);

        c = configKeeper.getConfiguratie("showLegendInTree", rolnaam);
        writeBoolean(dynaForm, "cfg_showLegendInTree", c);

        c = configKeeper.getConfiguratie("useMouseOverTabs", rolnaam);
        writeBoolean(dynaForm, "cfg_useMouseOverTabs", c);

        c = configKeeper.getConfiguratie("layoutAdminData", rolnaam);
        writeString(dynaForm, "cfg_layoutAdminData", c);

        c = configKeeper.getConfiguratie("showRedliningTools", rolnaam);
        writeBoolean(dynaForm, "cfg_showRedliningTools", c);

        c = configKeeper.getConfiguratie("showBufferTool", rolnaam);
        writeBoolean(dynaForm, "cfg_showBufferTool", c);

        c = configKeeper.getConfiguratie("showSelectBulkTool", rolnaam);
        writeBoolean(dynaForm, "cfg_showSelectBulkTool", c);

        c = configKeeper.getConfiguratie("showNeedleTool", rolnaam);
        writeBoolean(dynaForm, "cfg_showNeedleTool", c);

        c = configKeeper.getConfiguratie("layerGrouping", rolnaam);
        writeString(dynaForm, "cfg_layerGrouping", c);

        c = configKeeper.getConfiguratie("popupWidth", rolnaam);
        writeString(dynaForm, "cfg_popupWidth", c);

        c = configKeeper.getConfiguratie("popupHeight", rolnaam);
        writeString(dynaForm, "cfg_popupHeight", c);

        c = configKeeper.getConfiguratie("popupLeft", rolnaam);
        writeString(dynaForm, "cfg_popupLeft", c);

        c = configKeeper.getConfiguratie("popupTop", rolnaam);
        writeString(dynaForm, "cfg_popupTop", c);

        c = configKeeper.getConfiguratie("defaultdataframehoogte", rolnaam);
        writeString(dynaForm, "cfg_defaultdataframehoogte", c);

        /* opslaan zoekinganen */
        writeZoekenIdConfig(dynaForm, rolnaam);
        writePlanSelectieIdConfig(dynaForm, rolnaam);
        writeVoorzieningIdConfig(dynaForm, rolnaam);
        writeVergunningIdConfig(dynaForm, rolnaam);
    }

    private void writeZoekenIdConfig(DynaValidatorForm form, String rolnaam) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("zoekConfigIds", rolnaam);
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
    
    private void writeVoorzieningIdConfig(DynaValidatorForm form, String rolnaam) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("voorzieningConfigIds", rolnaam);
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
        
        
        Configuratie configStraal = configKeeper.getConfiguratie("voorzieningConfigStraal", rolnaam);
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

        Configuratie configType = configKeeper.getConfiguratie("voorzieningConfigTypes", rolnaam);
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

    private void writeVergunningIdConfig(DynaValidatorForm form, String rolnaam) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("vergunningConfigIds", rolnaam);
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

        lastComma = strBeheerTabs.lastIndexOf(",");

        if (lastComma > 1) {
            strBeheerTabs = strBeheerTabs.substring(0, lastComma);
        }

        strBeheerTabs = "\"" + strBeheerTabs + "\"";

        configTabs.setPropval(strBeheerTabs);
        configTabs.setType("java.lang.String");
        sess.merge(configTabs);
        sess.flush();


        Configuratie configStraal = configKeeper.getConfiguratie("vergunningConfigStraal", rolnaam);
        String straal = "";
        if (!form.get("cfg_vergunningstraal").equals("leeg")) {
            straal = form.get("cfg_vergunningstraal") + "";
        }
        straal = "\"" + straal + "\"";

        configStraal.setPropval(straal);
        configStraal.setType("java.lang.String");
        sess.merge(configStraal);
        sess.flush();


        int lastTypComma = -1;

        Configuratie configType = configKeeper.getConfiguratie("vergunningConfigTypes", rolnaam);
        String strTypeTabs = "";

        if (!form.get("cfg_vergunningtype1").equals("leeg")) {
            strTypeTabs += form.get("cfg_vergunningtype1") + ",";
        }

        if (!form.get("cfg_vergunningtype2").equals("leeg")) {
            strTypeTabs += form.get("cfg_vergunningtype2") + ",";
        }

        if (!form.get("cfg_vergunningtype3").equals("leeg")) {
            strTypeTabs += form.get("cfg_vergunningtype3") + ",";
        }

        lastTypComma = strTypeTabs.lastIndexOf(",");

        if (lastTypComma > 1) {
            strTypeTabs = strTypeTabs.substring(0, lastTypComma);
        }

        strTypeTabs = "\"" + strTypeTabs + "\"";

        configType.setPropval(strTypeTabs);
        configType.setType("java.lang.String");
        sess.merge(configType);
        sess.flush();

        //veld
        int lastVeldComma = -1;

        Configuratie configVeld = configKeeper.getConfiguratie("vergunningConfigVeld", rolnaam);
        String strVeldTabs = "";

        if (!form.get("cfg_vergunningveld1").equals("leeg")) {
            strVeldTabs += form.get("cfg_vergunningveld1") + ",";
        }

        if (!form.get("cfg_vergunningveld2").equals("leeg")) {
            strVeldTabs += form.get("cfg_vergunningveld2") + ",";
        }

        if (!form.get("cfg_vergunningveld3").equals("leeg")) {
            strVeldTabs += form.get("cfg_vergunningveld3") + ",";
        }

        lastVeldComma = strVeldTabs.lastIndexOf(",");

        if (lastVeldComma > 1) {
            strVeldTabs = strVeldTabs.substring(0, lastVeldComma);
        }

        strVeldTabs = "\"" + strVeldTabs + "\"";

        configVeld.setPropval(strVeldTabs);
        configVeld.setType("java.lang.String");
        sess.merge(configVeld);
        sess.flush();

        //term
        int lastTermComma = -1;

        Configuratie configTerm = configKeeper.getConfiguratie("vergunningConfigTerm", rolnaam);
        String strTermTabs = "";

        if (!form.get("cfg_vergunningterm1").equals("leeg")) {
            strTermTabs += form.get("cfg_vergunningterm1") + ",";
        }

        if (!form.get("cfg_vergunningterm2").equals("leeg")) {
            strTermTabs += form.get("cfg_vergunningterm2") + ",";
        }

        if (!form.get("cfg_vergunningterm3").equals("leeg")) {
            strTermTabs += form.get("cfg_vergunningterm3") + ",";
        }

        lastTermComma = strTermTabs.lastIndexOf(",");

        if (lastTermComma > 1) {
            strTermTabs = strTermTabs.substring(0, lastTermComma);
        }

        strTermTabs = "\"" + strTermTabs + "\"";

        configTerm.setPropval(strTermTabs);
        configTerm.setType("java.lang.String");
        sess.merge(configTerm);
        sess.flush();
    }

    private void writePlanSelectieIdConfig(DynaValidatorForm form, String rolnaam) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("planSelectieIds", rolnaam);
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

    private void writeTabbladenConfig(DynaValidatorForm form, String rolnaam) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        Configuratie configTabs = configKeeper.getConfiguratie("tabs", rolnaam);
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

        configTabs.setPropval(strBeheerTabs);
        configTabs.setType("java.lang.String");
        sess.merge(configTabs);
        sess.flush();
    }

    private void writeBoolean(DynaValidatorForm form, String field, Configuratie c) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        if (form.get(field) != null && form.get(field).toString().length()>0) {
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
        if (tabs==null) {
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

    private void writeObjectInfoDisplayMethod(DynaValidatorForm form, String rolnaam) {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();

        Configuratie usePopup = configKeeper.getConfiguratie("usePopup", rolnaam);
        usePopup.setType("java.lang.Boolean");
        Configuratie useDivPopup = configKeeper.getConfiguratie("useDivPopup", rolnaam);
        useDivPopup.setType("java.lang.Boolean");
        Configuratie usePanelControls = configKeeper.getConfiguratie("usePanelControls", rolnaam);
        usePanelControls.setType("java.lang.Boolean");

        // geen, paneel, popup
        String cfg_objectInfo = (String) form.get("cfg_objectInfo");

        if (cfg_objectInfo.equals("geen")) {
            usePopup.setPropval("false");
            useDivPopup.setPropval("false");
            usePanelControls.setPropval("false");

            sess.merge(usePopup);
            sess.merge(useDivPopup);
            sess.merge(usePanelControls);

            sess.flush();
        }

        if (cfg_objectInfo.equals("paneel")) {
            usePopup.setPropval("false");
            useDivPopup.setPropval("false");
            usePanelControls.setPropval("true");

            sess.merge(usePopup);
            sess.merge(useDivPopup);
            sess.merge(usePanelControls);

            sess.flush();
        }

        if (cfg_objectInfo.equals("popup")) {
            usePopup.setPropval("true");
            useDivPopup.setPropval("true");
            usePanelControls.setPropval("false");

            sess.merge(usePopup);
            sess.merge(useDivPopup);
            sess.merge(usePanelControls);

            sess.flush();
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

        if(voorzieningConfigStraal != null){
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
            HttpServletRequest request, String ids, String vergunningConfigStraal, String vergunningConfigTypes, String vergunningConfigVeld, String vergunningConfigTerm) {

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
        }

        if(vergunningConfigStraal != null){
            dynaForm.set("cfg_vergunningstraal", vergunningConfigStraal.replaceAll("\"", ""));
        }

        if (vergunningConfigTypes != null) {
            String[] typeItems = vergunningConfigTypes.replaceAll("\"", "").split(",");

            if (typeItems.length > 0) {
                if ((typeItems[0] != null) || typeItems[0].equals("")) {
                    dynaForm.set("cfg_vergunningtype1", typeItems[0].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 1) {
                if ((typeItems[1] != null) || typeItems[1].equals("")) {
                    dynaForm.set("cfg_vergunningtype2", typeItems[1].replaceAll("\"", ""));
                }
            }

            if (typeItems.length > 2) {
                if ((typeItems[2] != null) || typeItems[2].equals("")) {
                    dynaForm.set("cfg_vergunningtype3", typeItems[2].replaceAll("\"", ""));
                }
            }
        }


        if (vergunningConfigVeld != null) {
            String[] veldItems = vergunningConfigVeld.replaceAll("\"", "").split(",");

            if (veldItems.length > 0) {
                if ((veldItems[0] != null) || veldItems[0].equals("")) {
                    dynaForm.set("cfg_vergunningveld1", veldItems[0].replaceAll("\"", ""));
                }
            }

            if (veldItems.length > 1) {
                if ((veldItems[1] != null) || veldItems[1].equals("")) {
                    dynaForm.set("cfg_vergunningveld2", veldItems[1].replaceAll("\"", ""));
                }
            }

            if (veldItems.length > 2) {
                if ((veldItems[2] != null) || veldItems[2].equals("")) {
                    dynaForm.set("cfg_vergunningveld3", veldItems[2].replaceAll("\"", ""));
                }
            }
        }


        if (vergunningConfigTerm != null) {
            String[] termItems = vergunningConfigTerm.replaceAll("\"", "").split(",");

            if (termItems.length > 0) {
                if ((termItems[0] != null) || termItems[0].equals("")) {
                    dynaForm.set("cfg_vergunningterm1", termItems[0].replaceAll("\"", ""));
                }
            }

            if (termItems.length > 1) {
                if ((termItems[1] != null) || termItems[1].equals("")) {
                    dynaForm.set("cfg_vergunningterm2", termItems[1].replaceAll("\"", ""));
                }
            }

            if (termItems.length > 2) {
                if ((termItems[2] != null) || termItems[2].equals("")) {
                    dynaForm.set("cfg_vergunningterm3", termItems[2].replaceAll("\"", ""));
                }
            }
        }

    }

    private void writeDefaultConfigForRole(String rol) {

        /* Invoegen default config voor rolnaam */
        Configuratie cfg = null;
     
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            
        cfg = new Configuratie();
        cfg.setProperty("useCookies");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("multipleActiveThemas");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("dataframepopupHandle");
        cfg.setPropval("null");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLeftPanel");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("autoRedirect");
        cfg.setPropval("2");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useSortableFunction");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layerDelay");
        cfg.setPropval("5000");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("refreshDelay");
        cfg.setPropval("1000");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("minBboxZoeken");
        cfg.setPropval("1000");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("zoekConfigIds");
        cfg.setPropval("\"-1\"");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("maxResults");
        cfg.setPropval("25");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("usePopup");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useDivPopup");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("usePanelControls");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("expandAll");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tolerance");
        cfg.setPropval("1");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useInheritCheckbox");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showLegendInTree");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("useMouseOverTabs");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("layoutAdminData");
        cfg.setPropval("admindata1");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("tabs");
        cfg.setPropval("\"themas\",\"legenda\",\"zoeken\"");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("planSelectieIds");
        cfg.setPropval("-1");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showRedliningTools");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showBufferTool");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showSelectBulkTool");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("showNeedleTool");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);
        
        cfg = new Configuratie();
        cfg.setProperty("layerGrouping");
        cfg.setPropval("lg_forebackground");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupWidth");
        cfg.setPropval("90%");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupHeight");
        cfg.setPropval("20%");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupLeft");
        cfg.setPropval("5%");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("popupTop");
        cfg.setPropval("75%");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);

        cfg = new Configuratie();
        cfg.setProperty("defaultdataframehoogte");
        cfg.setPropval("150");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");
        sess.save(cfg);
        
        sess.flush();
    }
}