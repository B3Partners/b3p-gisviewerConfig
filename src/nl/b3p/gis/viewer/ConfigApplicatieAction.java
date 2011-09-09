package nl.b3p.gis.viewer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.commons.services.FormUtils;
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

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* Applicaties ophalen */
        list(request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    private void list(HttpServletRequest request) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        List<Applicatie> applicaties = sess.createQuery("from Applicatie order by id").list();
        
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

        /* Applicatie opslaan */
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Applicatie app = getApplicatie(dynaForm, request);

        populateObject(dynaForm, app, request);
        sess.saveOrUpdate(app);
        sess.flush();

        /* Applicaties ophalen */
        list(request);
        
        /* Klaarzetten form */
        populateForm(app, dynaForm, request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return mapping.findForward(SUCCESS);
    }

    @Override
    public ActionForward edit(ActionMapping mapping, DynaValidatorForm dynaForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Applicatie app = getApplicatie(dynaForm, request);
        populateForm(app, dynaForm, request);

        /* Applicatie bijwerken */

        /* Applicaties ophalen */
        list(request);

        prepareMethod(dynaForm, request, EDIT, LIST);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        /* Applicatie verwijderen */

        /* Applicaties ophalen */
        list(request);

        dynaForm.initialize(mapping);
        prepareMethod(dynaForm, request, LIST, EDIT);
        addDefaultMessage(mapping, request, ACKNOWLEDGE_MESSAGES);

        return getDefaultForward(mapping, request);
    }
    
    private Applicatie getApplicatie(DynaValidatorForm form, HttpServletRequest request) {
        Integer id = (Integer) form.get("applicatieID");

        if (id == null || id < 1) {
            id = (Integer) request.getAttribute("applicatieID");
        }

        if (id == null) {
            return new Applicatie();
        }

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();
        Applicatie app = (Applicatie) sess.get(Applicatie.class, id);

        return app;
    }

    private void populateForm(Applicatie app, DynaValidatorForm dynaForm, HttpServletRequest request) {
        if (app == null) {
            return;
        }

        dynaForm.set("applicatieID", app.getId());
        dynaForm.set("naam", app.getNaam());
        dynaForm.set("code", app.getCode());
        dynaForm.set("gebruikersCode", app.getGebruikersCode());
        dynaForm.set("parent", app.getParent());

        Date datum_gebruikt = app.getDatum_gebruikt();

        if (datum_gebruikt != null) {
            SimpleDateFormat df = new SimpleDateFormat("d-M-yyyy", new Locale("NL"));
            dynaForm.set("datum_gebruikt", df.format(datum_gebruikt));
        }
    }

    private void populateObject(DynaValidatorForm dynaForm, Applicatie app, HttpServletRequest request) {
        Integer id = (Integer) dynaForm.get("applicatieID");

        if (id != null && id != 0) {
            app.setId(id);
        }

        app.setNaam(FormUtils.nullIfEmpty(dynaForm.getString("naam")));
        app.setCode(FormUtils.nullIfEmpty(dynaForm.getString("code")));
        app.setGebruikersCode(FormUtils.nullIfEmpty(dynaForm.getString("gebruikersCode")));
        app.setParent(null);
        app.setDatum_gebruikt(new Date());
    }
}
