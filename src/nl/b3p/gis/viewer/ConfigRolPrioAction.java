package nl.b3p.gis.viewer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

public class ConfigRolPrioAction extends ViewerCrudAction {

    private static final Log logger = LogFactory.getLog(ConfigRolPrioAction.class);

    @Override
    public ActionForward unspecified(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        /* heeft de gebruiker op het pijltje geklikt om een item
         * te verplaatsen ? */
        String moveAction = (String)request.getParameter("moveAction");
        String nieuweRollen = "";

        if (moveAction != null && !moveAction.equals("")) {
            nieuweRollen = handleMoveAction(moveAction);
        }

        /* huidige beheerder rollen ophalen */
        GisPrincipal user = GisPrincipal.getGisPrincipal(request);
        Set roles = user.getRoles();

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = configKeeper.getConfiguratie("rollenPrio","rollen");

        /* indien nog geen rollenPrio config dan aanmaken met huidige
         beheerder rollen */
        if (rollenPrio == null) {
            rollenPrio = new Configuratie();
            rollenPrio.setProperty("rollenPrio");
            rollenPrio.setSetting("rollen");

            String strRoles = getCommaSeperatedValues(roles);
            rollenPrio.setPropval(strRoles);

            rollenPrio.setType("java.lang.String");

            sess.save(rollenPrio);
            sess.flush();
        }

        if (nieuweRollen.equals("")) {

            /* nieuwe rollen in config plaatsen */
            String configRollen = rollenPrio.getPropval();
            Iterator iter = roles.iterator();

            while (iter.hasNext()) {
                String rolnaam = iter.next().toString();

                if (!configRollen.contains(rolnaam)) {
                    configRollen += "," + rolnaam;
                }
            }
            
            rollenPrio.setPropval(configRollen);
            sess.merge(rollenPrio);
            sess.flush();

        } else {

            rollenPrio.setPropval(nieuweRollen);
            sess.merge(rollenPrio);
            sess.flush();
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

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        ConfigKeeper configKeeper = new ConfigKeeper();
        Configuratie rollenPrio = configKeeper.getConfiguratie("rollenPrio","rollen");

        String rollen = rollenPrio.getPropval();
        rollen += "," + nieuwe_rol;

        rollenPrio.setPropval(rollen);
        sess.merge(rollenPrio);
        sess.flush();

        /* rollen klaarzetten voor form */
        String[] arr = rollenPrio.getPropval().split(",");
        request.setAttribute("rollen", arr);
        
        return super.save(mapping, dynaForm, request, response);
    }

    @Override
    public ActionForward delete(ActionMapping mapping, DynaValidatorForm dynaForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String rol = (String)request.getParameter("delete");

        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

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

        rollenPrio.setPropval(strRollen);
        sess.merge(rollenPrio);
        sess.flush();

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

    private String handleMoveAction(String action) {
        String splitChar = "@@";

        String[] arr = action.split(splitChar);

        if (arr.length < 1 || arr.length > 2)
            return "";

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

        return str;
    }
}