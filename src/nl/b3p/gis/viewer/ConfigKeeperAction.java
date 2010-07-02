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

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {        

        /* rol ophalen */
        String rolnaam = (String)request.getParameter("rolnaam");

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap(rolnaam);

        if (map.size() < 1) {
            writeDeafultConfigForRole(rolnaam);
        } else {

            boolean useCookies = (Boolean) map.get("useCookies");
            boolean usePopup = (Boolean) map.get("usePopup");
            boolean useDivPopup = (Boolean) map.get("useDivPopup");
            boolean usePanelControls = (Boolean) map.get("usePanelControls");
            int autoRedirect = (Integer) map.get("autoRedirect");
            int tolerance = (Integer) map.get("tolerance");
            int refreshDelay = (Integer) map.get("refreshDelay");
            String zoekConfigIds = (String) map.get("zoekConfigIds");
            int minBboxZoeken = (Integer) map.get("minBboxZoeken");
            int maxResults = (Integer) map.get("maxResults");
            boolean expandAll = (Boolean) map.get("expandAll");
            boolean multipleActiveThemas = (Boolean) map.get("multipleActiveThemas");

            /* Vinkjes voor config tabbladen goedzetten */
            fillTabbladenConfig(dynaForm, map);

            /* dropdown voor i-tool goedzetten
               geen, paneel of popup */
            if (!usePopup && !useDivPopup && !usePanelControls)
                dynaForm.set("cfg_objectInfo", "geen");

            if (!usePopup && !useDivPopup && usePanelControls)
                dynaForm.set("cfg_objectInfo", "paneel");

            if (usePopup && useDivPopup && !usePanelControls)
                dynaForm.set("cfg_objectInfo", "popup");

            /* vullen box voor zoekconfigs */
            fillZoekConfigBox(dynaForm, request, zoekConfigIds);

            /* overige settings klaarzetten voor formulier */
            dynaForm.set("cfg_useCookies", useCookies);
            dynaForm.set("cfg_autoRedirect", autoRedirect);
            dynaForm.set("cfg_tolerance", tolerance);
            dynaForm.set("cfg_refreshDelay", refreshDelay);
            dynaForm.set("cfg_minBboxZoeken", minBboxZoeken);
            dynaForm.set("cfg_maxResults", maxResults);
            dynaForm.set("cfg_expandAll", expandAll);
            dynaForm.set("cfg_multipleActiveThemas", multipleActiveThemas);
        }

        return super.unspecified(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
  
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Opslaan tabbladen vinkjes */
        writeTabbladenConfig(dynaForm);

        /* opslaan I-tool dropdown */
        writeObjectInfoDisplayMethod(dynaForm);

        /* opslaan overige settings */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie c = null;

        c = configKeeper.getConfiguratie("useCookies","viewer");
        writeBoolean(dynaForm, "cfg_useCookies", c);

        c = configKeeper.getConfiguratie("autoRedirect","viewer");
        writeInteger(dynaForm, "cfg_autoRedirect", c);

        c = configKeeper.getConfiguratie("tolerance","viewer");
        writeInteger(dynaForm, "cfg_tolerance", c);

        c = configKeeper.getConfiguratie("refreshDelay","viewer");
        writeInteger(dynaForm, "cfg_refreshDelay", c);

        c = configKeeper.getConfiguratie("minBboxZoeken","viewer");
        writeInteger(dynaForm, "cfg_minBboxZoeken", c);

        c = configKeeper.getConfiguratie("maxResults","viewer");
        writeInteger(dynaForm, "cfg_maxResults", c);

        c = configKeeper.getConfiguratie("expandAll","viewer");
        writeBoolean(dynaForm, "cfg_expandAll", c);

        c = configKeeper.getConfiguratie("multipleActiveThemas","viewer");
        writeBoolean(dynaForm, "cfg_multipleActiveThemas", c);

        /* opslaan zoekconfig id's */
        String ids = "";

        if (dynaForm.get("zoekconfigids") != null) {         
            String[] arr = (String[]) dynaForm.get("zoekconfigids");

            for (int i=0; i < arr.length; i++) {
                ids += arr[i] + ",";
            }

            int lastComma = ids.lastIndexOf(",");

            if (lastComma > 0)
                ids = ids.substring(0, lastComma);

            ids = "\"" + ids + "\"";

            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

            Configuratie zoekConfigIds = configKeeper.getConfiguratie("zoekConfigIds","viewer");
            zoekConfigIds.setPropval(ids);

            sess.merge(zoekConfigIds);
            sess.flush();
        }

        /* vullen box voor zoekconfigs */
        fillZoekConfigBox(dynaForm, request, ids);
        
        return super.save(mapping, dynaForm, request, response);
    }

    private void writeTabbladenConfig(DynaValidatorForm form) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        int lastComma = -1;

        /* beheerder */
        Configuratie configBeheer = configKeeper.getConfiguratie("tabbladenBeheerder","viewer");
        String strBeheerTabs = "";

        if (form.get("cfg_tab_beh_thema") != null)
            strBeheerTabs += "\"themas\",";

        if (form.get("cfg_tab_beh_legenda") != null)
            strBeheerTabs += "\"legenda\",";

        if (form.get("cfg_tab_beh_zoek") != null)
            strBeheerTabs += "\"zoeken\",";

        if (form.get("cfg_tab_beh_info") != null)
            strBeheerTabs += "\"informatie\",";
        
        if (form.get("cfg_tab_beh_gebied") != null)
            strBeheerTabs += "\"gebieden\",";
        
        if (form.get("cfg_tab_beh_analyse") != null)
            strBeheerTabs += "\"analyse\",";
        
        if (form.get("cfg_tab_beh_plansel") != null)
            strBeheerTabs += "\"planselectie\",";

        lastComma = strBeheerTabs.lastIndexOf(",");

        if (lastComma > 1)
            strBeheerTabs = strBeheerTabs.substring(0, lastComma);

        configBeheer.setPropval(strBeheerTabs);
        sess.merge(configBeheer);
        
        /* gebruiker */
        Configuratie configGebruiker = configKeeper.getConfiguratie("tabbladenGebruiker","viewer");
        String strGebruikerTabs = "";

        if (form.get("cfg_tab_geb_thema") != null)
            strGebruikerTabs += "\"themas\",";

        if (form.get("cfg_tab_geb_legenda") != null)
            strGebruikerTabs += "\"legenda\",";

        if (form.get("cfg_tab_geb_zoek") != null)
            strGebruikerTabs += "\"zoeken\",";

        if (form.get("cfg_tab_geb_info") != null)
            strGebruikerTabs += "\"informatie\",";
        
        if (form.get("cfg_tab_geb_gebied") != null)
            strGebruikerTabs += "\"gebieden\",";
        
        if (form.get("cfg_tab_geb_analyse") != null)
            strGebruikerTabs += "\"analyse\",";
        
        if (form.get("cfg_tab_geb_plansel") != null)
            strGebruikerTabs += "\"planselectie\",";

        lastComma = strGebruikerTabs.lastIndexOf(",");

        if (lastComma > 1)
            strGebruikerTabs = strGebruikerTabs.substring(0, lastComma);

        configGebruiker.setPropval(strGebruikerTabs);
        sess.merge(configGebruiker);
        
        /* demo */
        Configuratie configDemo = configKeeper.getConfiguratie("tabbladenDemoGebruiker","viewer");
        String strDemoTabs = "";

        if (form.get("cfg_tab_dem_thema") != null)
            strDemoTabs += "\"themas\",";

        if (form.get("cfg_tab_dem_legenda") != null)
            strDemoTabs += "\"legenda\",";

        if (form.get("cfg_tab_dem_zoek") != null)
            strDemoTabs += "\"zoeken\",";

        if (form.get("cfg_tab_dem_info") != null)
            strDemoTabs += "\"informatie\",";
        
        if (form.get("cfg_tab_dem_gebied") != null)
            strDemoTabs += "\"gebieden\",";
        
        if (form.get("cfg_tab_dem_analyse") != null)
            strDemoTabs += "\"analyse\",";
        
        if (form.get("cfg_tab_dem_plansel") != null)
            strDemoTabs += "\"planselectie\",";

        lastComma = strDemoTabs.lastIndexOf(",");

        if (lastComma > 1)
            strDemoTabs = strDemoTabs.substring(0, lastComma);

        configDemo.setPropval(strDemoTabs);
        sess.merge(configDemo);
        
        /* demo */
        Configuratie configAnoniem = configKeeper.getConfiguratie("tabbladenAnoniem","viewer");
        String strAnomTabs = "";

        if (form.get("cfg_tab_ano_thema") != null)
            strAnomTabs += "\"themas\",";

        if (form.get("cfg_tab_ano_legenda") != null)
            strAnomTabs += "\"legenda\",";

        if (form.get("cfg_tab_ano_zoek") != null)
            strAnomTabs += "\"zoeken\",";

        if (form.get("cfg_tab_ano_info") != null)
            strAnomTabs += "\"informatie\",";
        
        if (form.get("cfg_tab_ano_gebied") != null)
            strAnomTabs += "\"gebieden\",";
        
        if (form.get("cfg_tab_ano_analyse") != null)
            strAnomTabs += "\"analyse\",";
        
        if (form.get("cfg_tab_ano_plansel") != null)
            strAnomTabs += "\"planselectie\",";

        lastComma = strAnomTabs.lastIndexOf(",");

        if (lastComma > 1)
            strAnomTabs = strAnomTabs.substring(0, lastComma);

        configAnoniem.setPropval(strAnomTabs);
        sess.merge(configAnoniem);

        sess.flush();
    }

    private void writeBoolean(DynaValidatorForm form, String field, Configuratie c) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        if (form.get(field) != null) {
            c.setPropval("true");
        } else {
            c.setPropval("false");
        }

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

        sess.merge(c);
        sess.flush();
    }

    private void fillTabbladenConfig(DynaValidatorForm dynaForm, Map map) {
        String tabs = (String) map.get("tabs");

        if (tabs.contains("themas"))
            dynaForm.set("cfg_tab_thema", true);

        if (tabs.contains("legenda"))
            dynaForm.set("cfg_tab_legenda", true);

        if (tabs.contains("zoeken"))
            dynaForm.set("cfg_tab_zoek", true);

        if (tabs.contains("informatie"))
            dynaForm.set("cfg_tab_info", true);

        if (tabs.contains("gebieden"))
            dynaForm.set("cfg_tab_gebied", true);

        if (tabs.contains("analyse"))
            dynaForm.set("cfg_tab_analyse", true);

        if (tabs.contains("planselectie"))
            dynaForm.set("cfg_tab_plansel", true);
    }

    private void writeObjectInfoDisplayMethod(DynaValidatorForm form) {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();

        Configuratie usePopup = configKeeper.getConfiguratie("usePopup","viewer");
        Configuratie useDivPopup = configKeeper.getConfiguratie("useDivPopup","viewer");
        Configuratie usePanelControls = configKeeper.getConfiguratie("usePanelControls","viewer");

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

        String[] tmp = ids.replaceAll("\"", "").split(",");
        dynaForm.set("zoekconfigids", tmp);
    }

    private void writeDeafultConfigForRole(String rol) {

        /* Invoegen default config voor rolnaam */
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie cfg = null;

        cfg = new Configuratie();
        cfg.setProperty("useCookies");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("multipleActiveThemas");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("dataframepopupHandle");
        cfg.setPropval("null");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("showLeftPanel");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("autoRedirect");
        cfg.setPropval("2");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("useSortableFunction");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("layerDelay");
        cfg.setPropval("5000");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("refreshDelay");
        cfg.setPropval("1000");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("minBboxZoeken");
        cfg.setPropval("1000");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("zoekConfigIds");
        cfg.setPropval("\"1\"");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("maxResults");
        cfg.setPropval("25");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("usePopup");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("useDivPopup");
        cfg.setPropval("false");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("usePanelControls");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("expandAll");
        cfg.setPropval("true");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Boolean");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("tabs");
        cfg.setPropval("\"themas\", \"legenda\", \"zoeken\"");
        cfg.setSetting(rol);
        cfg.setType("java.lang.String");

        sess.save(cfg);
        sess.flush();

        cfg = new Configuratie();
        cfg.setProperty("tolerance");
        cfg.setPropval("1");
        cfg.setSetting(rol);
        cfg.setType("java.lang.Integer");

        sess.save(cfg);
        sess.flush();
    }
}