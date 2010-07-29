package nl.b3p.gis.viewer;

import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nl.b3p.gis.utils.ConfigKeeper;
import nl.b3p.gis.viewer.db.Configuratie;
import nl.b3p.gis.viewer.services.GisPrincipal;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConfigRolPrioAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigRolPrioAction.class);
    protected static final String ERROR_ROLE = "error.role";

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* heeft de gebruiker op het pijltje geklikt om een item
         * te verplaatsen ? */
        String moveAction = (String)request.getParameter("moveAction");
        String alleRollen = "";

        if (moveAction != null && !moveAction.equals("")) {
            handleMoveAction(moveAction);
        }

        /* huidige kaartenbalie rollen ophalen */
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Set roles = user.getRoles();

        /* rollen uit config database ophalen */
        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = configKeeper.getConfiguratie("rollenPrio","rollen");

        /* indien nog geen rollenPrio config dan init met huidige
         beheerder rollen en opslaan in configkeerp tabel */
        if (rollenPrio == null) {
            rollenPrio = new Configuratie();
            rollenPrio.setProperty("rollenPrio");
            rollenPrio.setSetting("rollen");

            String strRoles = getCommaSeperatedValues(roles);
            rollenPrio.setPropval(strRoles);
            rollenPrio.setType("java.lang.String");

            Session sess = null;
            Transaction tx = null;

            try {
                sess = HibernateUtil.getSessionFactory().openSession();
                tx = sess.beginTransaction();

                sess.save(rollenPrio);

                tx.commit();

            } catch (Exception ex) {
                logger.error("Fout bij opslaan nieuwe rollen config: " + ex.getLocalizedMessage());
                tx.rollback();
            }
        }

        /* bekijken of er een nieuwe rol in kaartenbalie is toegewezen
         * deze dan toevoegen aan config rollen */
        String configRollen = rollenPrio.getPropval();
        Iterator iter = roles.iterator();

        int count = 0;

        while (iter.hasNext()) {
            String rolnaam = iter.next().toString();

            if (!configRollen.contains(rolnaam)) {
                configRollen += "," + rolnaam;
                count++;
            }
        }

        /* indien nieuwe rol dan weer opslaan in configkpeer tabel */
        if (count > 0) {
            Session sess = null;
            Transaction tx = null;

            try {
                sess = HibernateUtil.getSessionFactory().openSession();
                tx = sess.beginTransaction();

                rollenPrio.setPropval(configRollen);
                sess.merge(rollenPrio);

                tx.commit();

            } catch (Exception ex) {
                logger.error("Fout bij opslaan na samengevoegde rollen config: " + ex.getLocalizedMessage());
                tx.rollback();
            }
        }

        /* rollen klaarzetten voor form */
        String[] rollen = rollenPrio.getPropval().split(",");
        request.setAttribute("rollen", rollen);

        return super.unspecified(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward save(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (!isTokenValid(request)) {

            addAlternateMessage(mapping, request, TOKEN_ERROR_KEY);
            return getAlternateForward(mapping, request);
        }

        String nieuwe_rol = (String)dynaForm.get("nieuwe_rol");

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = configKeeper.getConfiguratie("rollenPrio","rollen");

        String rollen = rollenPrio.getPropval();
        rollen += "," + nieuwe_rol;

        /* rol met naam default mag niet worden opgeslagen */
        if ( (nieuwe_rol == null) || (nieuwe_rol.equals("default")) || (nieuwe_rol.equals("")) ) {
            addAlternateMessage(mapping, request, ERROR_ROLE);

            /* rollen klaarzetten voor form */
            String[] arr = rollenPrio.getPropval().split(",");
            request.setAttribute("rollen", arr);

            return getAlternateForward(mapping, request);
        }

        Session sess = null;
        Transaction tx = null;

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            tx = sess.beginTransaction();

            rollenPrio.setPropval(rollen);
            sess.merge(rollenPrio);

            tx.commit();

        } catch (Exception ex) {
            logger.error("Fout bij opslaan nieuwe rol: " + ex.getLocalizedMessage());
            tx.rollback();
        }

        /* rollen klaarzetten voor form */
        String[] arr = rollenPrio.getPropval().split(",");
        request.setAttribute("rollen", arr);
        
        return super.save(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String rol = (String)request.getParameter("delete");

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = configKeeper.getConfiguratie("rollenPrio","rollen");

        String[] list = rollenPrio.getPropval().split(",");
        String strRollen = "";

        for (int i=0; i<list.length;i++) {

            if (!list[i].equals(rol)) {
                strRollen += list[i] + ",";
            }
        }

        int lastComma = -1;
        lastComma = strRollen.lastIndexOf(",");

        if (lastComma > 1)
            strRollen = strRollen.substring(0, lastComma);

        Session sess = null;
        Transaction tx = null;

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            tx = sess.beginTransaction();

            rollenPrio.setPropval(strRollen);
            sess.merge(rollenPrio);

            int query = sess.createSQLQuery("delete from configuratie" +
                " where setting = :rolnaam")
                .setParameter("rolnaam", rol).executeUpdate();

            tx.commit();

        } catch (Exception ex) {
            logger.error("Fout bij verwijderen rol: " + ex.getLocalizedMessage());
            tx.rollback();
        }

        /* rollen klaarzetten voor form */
        String[] arr = rollenPrio.getPropval().split(",");
        request.setAttribute("rollen", arr);

        return super.delete(mapping, dynaForm, request, response);

    }
    
    public ActionForward config(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        return config(mapping, dynaForm, request, response);
    }

    private String getCommaSeperatedValues(Set set) {
        String str = "";

        if (set != null) {
            Iterator iter = set.iterator();

            while (iter.hasNext()) {
                str += iter.next().toString() + ",";
            }
        }

        int lastComma = -1;
        lastComma = str.lastIndexOf(",");

        if (lastComma > 1)
            str = str.substring(0, lastComma);

        return str;
    }

    private void handleMoveAction(String action) {
        String splitChar = "@@";

        String[] arr = action.split(splitChar);

        if (arr.length < 1 || arr.length > 2)
            return;

        String rolnaam = arr[0];
        String move = arr[1];

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = configKeeper.getConfiguratie("rollenPrio","rollen");

        /* rollen verschuiven */
        String[] list = rollenPrio.getPropval().split(",");

        int currpos = -1;
        for (int i=0;i<list.length;i++) {
            if (list[i].equals(rolnaam))
                currpos = i;
        }

        if (move.equals("UP") && currpos > 0) {
            list[currpos] = list[currpos-1];
            list[currpos-1] = rolnaam;
        }

        if (move.equals("DOWN") && currpos < list.length-1) {
            list[currpos] = list[currpos+1];
            list[currpos+1] = rolnaam;
        }

        /* weer commaseperated opslaan */
        String str = "";

        for (int i=0;i<list.length;i++)
            str += list[i] + ",";

        int lastComma = -1;
        lastComma = str.lastIndexOf(",");

        if (lastComma > 1)
            str = str.substring(0, lastComma);

        Session sess = null;
        Transaction tx = null;

        try {
            sess = HibernateUtil.getSessionFactory().openSession();
            tx = sess.beginTransaction();

            rollenPrio.setPropval(str);
            sess.merge(rollenPrio);

            tx.commit();

        } catch (Exception ex) {
            logger.error("Fout bij opslaan na move rollen: " + ex.getLocalizedMessage());
            tx.rollback();
        }
    }
}