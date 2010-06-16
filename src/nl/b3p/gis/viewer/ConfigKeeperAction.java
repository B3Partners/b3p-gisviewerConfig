package nl.b3p.gis.viewer;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.utils.ConfigKeeper;
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

        /* Updaten config in db
        ConfigKeeper configKeeper = Conf

        populateClustersObject(dynaForm, c, request);

        sess.saveOrUpdate(c);
        sess.flush();
        */

        return super.save(mapping, dynaForm, request, response);
    }
}