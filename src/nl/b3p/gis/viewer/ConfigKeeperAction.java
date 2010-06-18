package nl.b3p.gis.viewer;

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

        ConfigKeeper configKeeper = new ConfigKeeper();
        Map map = configKeeper.getConfigMap("viewer");

        /* de getConfigMap geeft alle waardes terug volgens de types
         * zoals deze gedefinieerd zijn in het type kolom van de configuratie */
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

        /* overige settings klaarzetten voor formulier */
        dynaForm.set("cfg_useCookies", useCookies);
        dynaForm.set("cfg_autoRedirect", autoRedirect);
        dynaForm.set("cfg_tolerance", tolerance);
        dynaForm.set("cfg_refreshDelay", refreshDelay);
        dynaForm.set("cfg_zoekConfigIds", zoekConfigIds);
        dynaForm.set("cfg_minBboxZoeken", minBboxZoeken);
        dynaForm.set("cfg_maxResults", maxResults);
        dynaForm.set("cfg_expandAll", expandAll);

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

        c = configKeeper.getConfiguratie("zoekConfigIds","viewer");
        writeString(dynaForm, "cfg_zoekConfigIds", c);

        c = configKeeper.getConfiguratie("minBboxZoeken","viewer");
        writeInteger(dynaForm, "cfg_minBboxZoeken", c);

        c = configKeeper.getConfiguratie("maxResults","viewer");
        writeInteger(dynaForm, "cfg_maxResults", c);

        c = configKeeper.getConfiguratie("expandAll","viewer");
        writeBoolean(dynaForm, "cfg_expandAll", c);

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
        String beheerderTabs = (String) map.get("tabbladenBeheerder");
        String gebruikerTabs = (String) map.get("tabbladenGebruiker");
        String demoTabs = (String) map.get("tabbladenDemoGebruiker");
        String anoniemTabs = (String) map.get("tabbladenAnoniem");

        /* beheerder */
        if (beheerderTabs.contains("themas"))
            dynaForm.set("cfg_tab_beh_thema", true);

        if (beheerderTabs.contains("legenda"))
            dynaForm.set("cfg_tab_beh_legenda", true);

        if (beheerderTabs.contains("zoeken"))
            dynaForm.set("cfg_tab_beh_zoek", true);

        if (beheerderTabs.contains("informatie"))
            dynaForm.set("cfg_tab_beh_info", true);

        if (beheerderTabs.contains("gebieden"))
            dynaForm.set("cfg_tab_beh_gebied", true);

        if (beheerderTabs.contains("analyse"))
            dynaForm.set("cfg_tab_beh_analyse", true);

        if (beheerderTabs.contains("planselectie"))
            dynaForm.set("cfg_tab_beh_plansel", true);

        /* gebruiker */
        if (gebruikerTabs.contains("themas"))
            dynaForm.set("cfg_tab_geb_thema", true);

        if (gebruikerTabs.contains("legenda"))
            dynaForm.set("cfg_tab_geb_legenda", true);

        if (gebruikerTabs.contains("zoeken"))
            dynaForm.set("cfg_tab_geb_zoek", true);

        if (gebruikerTabs.contains("informatie"))
            dynaForm.set("cfg_tab_geb_info", true);

        if (gebruikerTabs.contains("gebieden"))
            dynaForm.set("cfg_tab_geb_gebied", true);

        if (gebruikerTabs.contains("analyse"))
            dynaForm.set("cfg_tab_geb_analyse", true);

        if (gebruikerTabs.contains("planselectie"))
            dynaForm.set("cfg_tab_geb_plansel", true);

        /* demo */
        if (demoTabs.contains("themas"))
            dynaForm.set("cfg_tab_dem_thema", true);

        if (demoTabs.contains("legenda"))
            dynaForm.set("cfg_tab_dem_legenda", true);

        if (demoTabs.contains("zoeken"))
            dynaForm.set("cfg_tab_dem_zoek", true);

        if (demoTabs.contains("informatie"))
            dynaForm.set("cfg_tab_dem_info", true);

        if (demoTabs.contains("gebieden"))
            dynaForm.set("cfg_tab_dem_gebied", true);

        if (demoTabs.contains("analyse"))
            dynaForm.set("cfg_tab_dem_analyse", true);

        if (demoTabs.contains("planselectie"))
            dynaForm.set("cfg_tab_dem_plansel", true);

        /* anoniem */
        if (anoniemTabs.contains("themas"))
            dynaForm.set("cfg_tab_ano_thema", true);

        if (anoniemTabs.contains("legenda"))
            dynaForm.set("cfg_tab_ano_legenda", true);

        if (anoniemTabs.contains("zoeken"))
            dynaForm.set("cfg_tab_ano_zoek", true);

        if (anoniemTabs.contains("informatie"))
            dynaForm.set("cfg_tab_ano_info", true);

        if (anoniemTabs.contains("gebieden"))
            dynaForm.set("cfg_tab_ano_gebied", true);

        if (anoniemTabs.contains("analyse"))
            dynaForm.set("cfg_tab_ano_analyse", true);

        if (anoniemTabs.contains("planselectie"))
            dynaForm.set("cfg_tab_ano_plansel", true);
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
}