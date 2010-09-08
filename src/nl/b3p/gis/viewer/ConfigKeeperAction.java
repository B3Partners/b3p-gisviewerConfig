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
import org.hibernate.Transaction;

public class ConfigKeeperAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigKeeperAction.class);
    private static final String[] CONFIGKEEPER_TABS = {
        "leeg", "themas", "legenda", "zoeken", "informatie", "gebieden",
        "analyse", "planselectie"
    };
    private static final String[] LABELS_VOOR_TABS = {
        "-Kies een tabblad-", "Kaarten", "Legenda", "Zoeken", "Info", "Gebieden",
        "Analyse", "Plannen"
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
        Boolean hideAdvancedButtons = (Boolean) map.get("hideAdvancedButtons");

        /* Tabbladen vullen */
        fillTabbladenConfig(dynaForm, map);

        request.setAttribute("tabValues", CONFIGKEEPER_TABS);
        request.setAttribute("tabLabels", LABELS_VOOR_TABS);

        /* dropdown voor i-tool goedzetten
        geen, paneel of popup */
        if (!usePopup && !useDivPopup && !usePanelControls) {
            dynaForm.set("cfg_objectInfo", "geen");
        }

        if (!usePopup && !useDivPopup && usePanelControls) {
            dynaForm.set("cfg_objectInfo", "paneel");
        }

        if (usePopup && useDivPopup && !usePanelControls) {
            dynaForm.set("cfg_objectInfo", "popup");
        }

        /* vullen box voor zoek ingangen */
        fillZoekConfigBox(dynaForm, request, zoekConfigIds);
        fillPlanSelectieBox(dynaForm, request, planSelectieIds);

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
        dynaForm.set("cfg_hideAdvancedButtons", hideAdvancedButtons);

        dynaForm.set("rolnaam", rolnaam);

        return super.unspecified(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String rolnaam = (String) dynaForm.get("rolnaam");

        if (!isTokenValid(request)) {

            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

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

        c = configKeeper.getConfiguratie("hideAdvancedButtons", rolnaam);
        writeBoolean(dynaForm, "cfg_hideAdvancedButtons", c);

        /* opslaan zoekinganen */
        writeZoekenIdConfig(dynaForm, rolnaam);
        writePlanSelectieIdConfig(dynaForm, rolnaam);

        /* opnieuw vullen box voor zoekconfigs */
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();

        request.setAttribute("zoekConfigs", zoekconfigs);

        /* opnieuwe klaarzetten zoekinganen */
        String cfg_zoekenid1 = (String) dynaForm.get("cfg_zoekenid1");
        String cfg_zoekenid2 = (String) dynaForm.get("cfg_zoekenid2");
        String cfg_zoekenid3 = (String) dynaForm.get("cfg_zoekenid3");
        String cfg_zoekenid4 = (String) dynaForm.get("cfg_zoekenid4");

        dynaForm.set("cfg_zoekenid1", cfg_zoekenid1);
        dynaForm.set("cfg_zoekenid2", cfg_zoekenid2);
        dynaForm.set("cfg_zoekenid3", cfg_zoekenid3);
        dynaForm.set("cfg_zoekenid4", cfg_zoekenid4);

        String cfg_planselectieid1 = (String) dynaForm.get("cfg_planselectieid1");
        String cfg_planselectieid2 = (String) dynaForm.get("cfg_planselectieid2");

        dynaForm.set("cfg_planselectieid1", cfg_planselectieid1);
        dynaForm.set("cfg_planselectieid2", cfg_planselectieid2);

        /* vullen dropdowns voor tabbladen */
        request.setAttribute("tabValues", CONFIGKEEPER_TABS);
        request.setAttribute("tabLabels", LABELS_VOOR_TABS);

        /* opnieuw vullen box voor tabs */
        String cfg_tab1 = (String) dynaForm.get("cfg_tab1");
        String cfg_tab2 = (String) dynaForm.get("cfg_tab2");
        String cfg_tab3 = (String) dynaForm.get("cfg_tab3");
        String cfg_tab4 = (String) dynaForm.get("cfg_tab4");
        String cfg_tab5 = (String) dynaForm.get("cfg_tab5");

        dynaForm.set("cfg_tab1", cfg_tab1);
        dynaForm.set("cfg_tab2", cfg_tab2);
        dynaForm.set("cfg_tab3", cfg_tab3);
        dynaForm.set("cfg_tab4", cfg_tab4);
        dynaForm.set("cfg_tab5", cfg_tab5);

        return super.save(mapping, dynaForm, request, response);
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

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        List zoekconfigs = sess.createQuery("from ZoekConfiguratie order by naam").list();

        request.setAttribute("zoekConfigs", zoekconfigs);

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
        cfg.setProperty("hideAdvancedButtons");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");
        sess.save(cfg);

        sess.flush();
    }
}
