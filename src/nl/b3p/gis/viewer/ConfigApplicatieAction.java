package nl.b3p.gis.viewer;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
import nl.b3p.commons.struts.ExtendedMethodProperties;
import nl.b3p.gis.utils.KaartSelectieUtil;
import nl.b3p.gis.viewer.db.Applicatie;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;

public class ConfigApplicatieAction extends ViewerCrudAction {

    private static final Log log = LogFactory.getLog(ConfigApplicatieAction.class);

    protected static final String MAPPING_COPY = "copy";

    @Override
    protected Map getActionMethodPropertiesMap() {
        Map map = super.getActionMethodPropertiesMap();

        ExtendedMethodProperties crudProp = null;

        crudProp = new ExtendedMethodProperties(MAPPING_COPY);
        crudProp.setDefaultForwardName(SUCCESS);
        crudProp.setDefaultMessageKey("message.applicatie.copy.success");
        crudProp.setAlternateForwardName(FAILURE);
        crudProp.setAlternateMessageKey("message.applicatie.copy.failed");
        map.put(MAPPING_COPY, crudProp);

        return map;
    }

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    protected void createLists(DynaValidatorForm form, HttpServletRequest request) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<Applicatie> applicaties = sess.createQuery("from Applicatie"
                + " order by user_copy, versie").list();

        if (applicaties != null && applicaties.size() > 0) {
            request.setAttribute("applicaties", applicaties);
        }
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Ald Applicatie is default app is aangevinkt eerst alle Applicaties
         * op default app false zetten */
        Boolean defaultApp = (Boolean) dynaForm.get("defaultApp");
        if (defaultApp != null) {
            updateAppsToNonDefault();
        }

        /* Applicatie ophalen en opslaan */
        Applicatie app = getApplicatie(dynaForm, request);
        populateObject(dynaForm, app, request);

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        sess.saveOrUpdate(app);
        sess.flush();
        
        /* Klaarzetten form */
        populateForm(app, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
                
        /* Applicatie ophalen en klaarzetten voor form */
        Applicatie app = getApplicatie(dynaForm, request);
        populateForm(app, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    public ActionForward copy(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Integer id = FormUtils.StringToInteger(request.getParameter("applicatieID"));

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Applicatie app = (Applicatie) sess.get(Applicatie.class, id);

        if (app == null) {
            addAlternateMessage(mapping, request, FAILURE);
            return getAlternateForward(mapping, request);
        }

        Applicatie newApp = KaartSelectieUtil.copyApplicatie(app, app.getRead_only(), app.getUser_copy());

        if (newApp == null) {
            addAlternateMessage(mapping, request, FAILURE);
            return getAlternateForward(mapping, request);
        }

        /* Vullen form met nieuwe Applicatie */
        populateForm(newApp, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, SUCCESS);

        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {
            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        /* Applicatie ophalen en verwijderen */
        Applicatie app = getApplicatie(dynaForm, request);

        if (app != null) {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            sess.delete(app);
            sess.flush();

            /* Configinstellingen, User Kaartgroepen en Kaartlagen verwijderen */
            KaartSelectieUtil.removeExistingConfigKeeperSettings(app.getCode());
            KaartSelectieUtil.removeExistingUserKaartgroepAndUserKaartlagen(app.getCode());
            KaartSelectieUtil.removeExistingUserServices(app.getCode());
        }

        dynaForm.initialize(mapping);

        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }
    
    private Applicatie getApplicatie(DynaValidatorForm dynaForm, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("applicatieID");

        Applicatie app = null;
        if (id == null || id < 1) {
            app = new Applicatie();

            String appCode = null;
            try {
                appCode = Applicatie.createApplicatieCode();
            } catch (Exception ex) {
                log.error("Fout tijdens maken Applicatie code:", ex);
            }

            app.setCode(appCode);
            
            /* Applicaties door de beheerder gemaakt zijn standaard read-only */
            app.setRead_only(true);
            app.setUser_copy(false);
            app.setDefault_app(false);
            app.setVersie(1);
        } else {
            Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
            app = (Applicatie) sess.get(Applicatie.class, id);
        }

        return app;
    }

    private void populateForm(Applicatie app, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (app == null) {
            return;
        }

        dynaForm.set("applicatieID", app.getId());
        dynaForm.set("naam", app.getNaam());
        dynaForm.set("gebruikersCode", app.getGebruikersCode());

        if (app.getParent() != null) {
            dynaForm.set("parent", app.getParent().getId());
        }

        dynaForm.set("defaultApp", app.getDefault_app());

        request.setAttribute("appcode", app.getCode());
    }

    private void populateObject(DynaValidatorForm dynaForm, Applicatie app, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("applicatieID");

        if (id != null && id != 0) {
            app.setId(id);
        }

        app.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        app.setGebruikersCode(FormUtils.nullIfEmpty(dynaForm.getString("gebruikersCode")));
        app.setParent(null);

        /* De gebruiksdatum alleen wijzigen als iemand de appcode gebruikt in de viewer */
        //app.setDatum_gebruikt(new Date());

        Boolean defaultApp = (Boolean) dynaForm.get("defaultApp");

        if (defaultApp != null) {          
            app.setDefault_app(true);
        } else {
            app.setDefault_app(false);
        }
    }

    private void updateAppsToNonDefault() {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<Applicatie> allApps = sess.createQuery("from Applicatie").list();

        for (Applicatie app : allApps) {
            app.setDefault_app(false);
            sess.save(app);
        }

        sess.flush();
    }
}
