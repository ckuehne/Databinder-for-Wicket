package net.databinder.models.hib;

import java.io.Serializable;

/*---
 Copyright 2008 The Scripps Research Institute
 http://www.scripps.edu

* Databinder: a simple bridge from Wicket to Hibernate
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* 
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 ---*/

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.core.util.lang.PropertyResolverConverter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.util.convert.ConversionException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

/**
 * An OrderingCriteriaBuilder implementation that can be wired to a FilterToolbar
 * String properties are searched via an iLike. Number properties can specify >, >=, < or <=
 * 
 * Example usage (from baseball player example);
 * 
 * CriteriaFilterAndSort builder = new CriteriaFilterAndSort(new Player(), "nameLast", true, false);
 * FilterForm form = new FilterForm("form", builder);
 * HibernateProvider provider = new HibernateProvider(Player.class, builder);
 * provider.setWrapWithPropertyModel(false);
 * DataTable table = new DataTable("players", columns, provider, 25) {
 *     @Override protected Item newRowItem(String id, int index, IModel model) {
 *         return new OddEvenItem(id, index, model);
 *     }
 * };
 * table.addTopToolbar(new AjaxNavigationToolbar(table));
 * table.addTopToolbar(new FilterToolbar(table, form, builder));
 * table.addTopToolbar(new AjaxFallbackHeadersToolbar(table, builder));
 * 
 * @author Mark Southern
 */
public class CriteriaFilterAndSort extends CriteriaBuildAndSort<Map<String, Serializable>> implements IFilterStateLocator<Map<String, Serializable>> {

    // whitespace, a qualifier, a number surrounded by whitespace
    private Pattern pattern = Pattern.compile("^(\\s+)?([><]=?)(\\s+)?(.*)(\\s+)?");

    private Map<String, Serializable> filterMap = new HashMap<>();

    private Serializable bean;

    /**
     * 
     * @param bean
     * @param defaultSortProperty
     * @param sortAscending
     * @param sortCased
     */
    public CriteriaFilterAndSort(Serializable bean, String defaultSortProperty, boolean sortAscending, boolean sortCased) {
        super(defaultSortProperty, sortAscending, sortCased);
        this.bean = bean;
    }

    public void buildUnordered(Criteria criteria) {
        super.buildUnordered(criteria);

        Conjunction conj = Restrictions.conjunction();

        for (Map.Entry<String, Serializable> entry : (Set<Map.Entry<String, Serializable>>) filterMap.entrySet()) {
            String property = entry.getKey();
            Serializable value = entry.getValue();
            if (value == null)
                continue;
            
			// for object filtered columns
			if (!(value instanceof String)) {
				conj.add(Restrictions.eq(property, value));
				criteria.add(conj);
				continue;
			}
            String stringValue = (String) value;

            String prop = processProperty(criteria, property);
            Class<?> clazz = PropertyResolver.getPropertyClass(property, bean);

            if (String.class.isAssignableFrom(clazz)) {
                String[] items = stringValue.split("\\s+");
                for (String item : items) {
                    Disjunction dist = Restrictions.disjunction();
                    dist.add(Restrictions.ilike(prop, item, MatchMode.ANYWHERE));
                    conj.add(dist);
                }
            }
            else if (Number.class.isAssignableFrom(clazz)) {
                try {
                    Matcher matcher = pattern.matcher(stringValue);
                    if (matcher.matches()) {
                        String qualifier = matcher.group(2);
                        stringValue = matcher.group(4);
                        Number num = convertToNumber(stringValue, clazz);
                        if (">".equals(qualifier))
                            conj.add(Restrictions.gt(prop, num));
                        else if ("<".equals(qualifier))
                            conj.add(Restrictions.lt(prop, num));
                        else if (">=".equals(qualifier))
                            conj.add(Restrictions.ge(prop, num));
                        else if ("<=".equals(qualifier))
                            conj.add(Restrictions.le(prop, num));
                    }
                    else
                        conj.add(Restrictions.eq(prop, convertToNumber(stringValue, clazz)));
                }
                catch(ConversionException ex) {
                    // ignore filter in this case
                }
            }
            else if (Boolean.class.isAssignableFrom(clazz)) {
                conj.add(Restrictions.eq(prop, Boolean.parseBoolean(stringValue)));
            }
        }
        criteria.add(conj);
    }
    
    protected Number convertToNumber(String value, Class<?> clazz) {
      return (Number)
        new PropertyResolverConverter(Application.get().getConverterLocator(), Session.get().getLocale())
          .convert(value, clazz);
    }
    
    public Map<String, Serializable> getFilterState() {
        return filterMap;
    }

	public void setFilterState(Map<String, Serializable> filterMap) {
        this.filterMap = filterMap;
    }

}