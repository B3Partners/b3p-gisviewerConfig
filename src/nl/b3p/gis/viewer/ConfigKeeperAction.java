package nl.b3p.gis.viewer;

import java.util.Collection;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
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
        boolean multipleActiveThemas = (Boolean) map.get("multipleActiveThemas");
        boolean usePopup = (Boolean) map.get("usePopup");
        boolean useDivPopup = (Boolean) map.get("useDivPopup");
        boolean dataframepopupHandle = (Boolean) map.get("dataframepopupHandle");
        boolean usePanelControls = (Boolean) map.get("usePanelControls");
        boolean showLeftPanel = (Boolean) map.get("showLeftPanel");
        int autoRedirect = (Integer) map.get("autoRedirect");
        int tolerance = (Integer) map.get("tolerance");
        boolean useSortableFunction = (Boolean) map.get("useSortableFunction");
        int layerDelay = (Integer) map.get("layerDelay");
        int refreshDelay = (Integer) map.get("refreshDelay");
        String zoekConfigIds = (String) map.get("zoekConfigIds");
        int minBboxZoeken = (Integer) map.get("minBboxZoeken");
        int maxResults = (Integer) map.get("maxResults");
        boolean expandAll = (Boolean) map.get("expandAll");
        String tabbladenBeheerder = (String) map.get("tabbladenBeheerder");
        String tabbladenGebruiker = (String) map.get("tabbladenGebruiker");
        String tabbladenDemoGebruiker = (String) map.get("tabbladenDemoGebruiker");
        String tabbladenAnoniem = (String) map.get("tabbladenAnoniem");

        /* config klaarzetten voor formulier */
        dynaForm.set("cfg_useCookies", useCookies);
        dynaForm.set("cfg_multipleActiveThemas", multipleActiveThemas);
        dynaForm.set("cfg_usePopup", usePopup);
        dynaForm.set("cfg_useDivPopup", useDivPopup);
        dynaForm.set("cfg_dataframepopupHandle", dataframepopupHandle);
        dynaForm.set("cfg_usePanelControls", usePanelControls);
        dynaForm.set("cfg_showLeftPanel", showLeftPanel);
        dynaForm.set("cfg_autoRedirect", autoRedirect);
        dynaForm.set("cfg_tolerance", tolerance);
        dynaForm.set("cfg_useSortableFunction", useSortableFunction);
        dynaForm.set("cfg_layerDelay", layerDelay);
        dynaForm.set("cfg_refreshDelay", refreshDelay);
        dynaForm.set("cfg_zoekConfigIds", zoekConfigIds);
        dynaForm.set("cfg_minBboxZoeken", minBboxZoeken);
        dynaForm.set("cfg_maxResults", maxResults);
        dynaForm.set("cfg_expandAll", expandAll);
        dynaForm.set("cfg_tabbladenBeheerder", tabbladenBeheerder);
        dynaForm.set("cfg_tabbladenGebruiker", tabbladenGebruiker);
        dynaForm.set("cfg_tabbladenDemoGebruiker", tabbladenDemoGebruiker);
        dynaForm.set("cfg_tabbladenAnoniem", tabbladenAnoniem);

        return super.unspecified(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
  
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie c = null;

        c = configKeeper.getConfiguratie("useCookies","viewer");
        writeBoolean(dynaForm, "cfg_useCookies", c);

        c = configKeeper.getConfiguratie("multipleActiveThemas","viewer");
        writeBoolean(dynaForm, "cfg_multipleActiveThemas", c);

        c = configKeeper.getConfiguratie("usePopup","viewer");
        writeBoolean(dynaForm, "cfg_usePopup", c);

        c = configKeeper.getConfiguratie("useDivPopup","viewer");
        writeBoolean(dynaForm, "cfg_useDivPopup", c);

        c = configKeeper.getConfiguratie("dataframepopupHandle","viewer");
        writeBoolean(dynaForm, "cfg_dataframepopupHandle", c);

        c = configKeeper.getConfiguratie("usePanelControls","viewer");
        writeBoolean(dynaForm, "cfg_usePanelControls", c);

        c = configKeeper.getConfiguratie("usePanelControls","viewer");
        writeBoolean(dynaForm, "cfg_showLeftPanel", c);

        c = configKeeper.getConfiguratie("autoRedirect","viewer");
        writeInteger(dynaForm, "cfg_autoRedirect", c);

        c = configKeeper.getConfiguratie("tolerance","viewer");
        writeInteger(dynaForm, "cfg_tolerance", c);

        c = configKeeper.getConfiguratie("useSortableFunction","viewer");
        writeBoolean(dynaForm, "cfg_useSortableFunction", c);

        c = configKeeper.getConfiguratie("layerDelay","viewer");
        writeInteger(dynaForm, "cfg_layerDelay", c);

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

        c = configKeeper.getConfiguratie("tabbladenBeheerder","viewer");
        writeString(dynaForm, "cfg_tabbladenBeheerder", c);

        c = configKeeper.getConfiguratie("tabbladenGebruiker","viewer");
        writeString(dynaForm, "cfg_tabbladenGebruiker", c);

        c = configKeeper.getConfiguratie("tabbladenDemoGebruiker","viewer");
        writeString(dynaForm, "cfg_tabbladenDemoGebruiker", c);

        c = configKeeper.getConfiguratie("tabbladenAnoniem","viewer");
        writeString(dynaForm, "cfg_tabbladenAnoniem", c);

        return super.save(mapping, dynaForm, request, response);
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
}