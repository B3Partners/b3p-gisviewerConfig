/*
 * B3P Gisviewer is an extension to Flamingo MapComponents making
 * it a complete webbased GIS viewer and configuration tool that
 * works in cooperation with B3P Kaartenbalie.
 *
 * Copyright 2006, 2007, 2008 B3Partners BV
 *
 * This file is part of B3P Gisviewer.
 *
 * B3P Gisviewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * B3P Gisviewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with B3P Gisviewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.gis.viewer;

import java.lang.reflect.Method;
import nl.b3p.gis.viewer.services.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.AjaxFilter;
import org.directwebremoting.AjaxFilterChain;
import org.directwebremoting.annotations.GlobalFilter;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * B3partners B.V. http://www.b3partners.nl
 * @author Roy
 * Created on 31-mei-2010, 9:47:02
 */
@GlobalFilter
public class DwrTransactionFilter implements AjaxFilter  {
    private static final Log log = LogFactory.getLog(DwrTransactionFilter.class);

    public Object doFilter(Object obj, Method method, Object[] params, AjaxFilterChain chain) throws Exception {
        Session sess = HibernateUtil.getSessionFactory().getCurrentSession();

        Transaction t=null;
        try {
            t=sess.beginTransaction();
            Object ret = chain.doFilter(obj, method, params);
            if(t != null && t.isActive())
                t.commit();
            return ret;
        }catch (Exception e) {
            log.error("Exception occured during DWR call" + (t.isActive() ? ", rolling back transaction" : " - no transaction active"), e);

            if(t.isActive()) {
                try {
                    t.rollback();
                } catch(Exception e2) {
                    /* log de exception maar swallow deze verder, omdat alleen
                     * wordt gerollback()'d indien er al een eerdere exception
                     * was gethrowed. Die wordt door deze te swallowen verder
                     * gethrowed.
                     */
                    log.error("Exception rolling back transaction", e2);
                }
            }
            throw e;		
        }finally {
            if(sess.isOpen())
                sess.close();
        }
    }
}
