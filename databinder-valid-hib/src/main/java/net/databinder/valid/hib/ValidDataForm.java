/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2008  Nathan Hamblen nathan@technically.us

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
 */
package net.databinder.valid.hib;

import java.io.Serializable;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

import net.databinder.components.hib.DataForm;
import net.databinder.models.hib.HibernateObjectModel;
import net.databinder.valid.hib.DatabinderValidator.UnrecognizedModelException;

/**
 * Form that adds a {@link DatabinderValidator} to all its components that
 * do not have any other validator in place. To exempt a component from
 * this validation, add to it the dummy validator returned by {@link #nonValidator()}.
 * Components are inspected in {@link #onBeforeRender()}. Those that do not have
 * a usable model (see {@link DatabinderValidator#DatabinderValidator()}
 * at that time are ignored.
 * @author Nathan Hamblen
 * @author Rodolfo Hansen
 * @param <T> the model object type
 * @see DatabinderValidator
 * @deprecated Use wicket-bean-validation project instead
 */
public class ValidDataForm<T> extends DataForm<T> {

  /** Hibernate Validator to use. */
  private Validator validator;

	/** */
  private static final long serialVersionUID = 1L;

  /**
	 * Instantiates this form and a new, blank instance of the given class as a persistent model
	 * object. By default the model object created is serialized and retained between requests until
	 * it is persisted.
	 * @param id wicket:id
	 * @param modelClass for the persistent object
	 * @see HibernateObjectModel#setRetainUnsaved(boolean)
	 */
	public ValidDataForm(final String id, final Class<T> modelClass) {
    super(id, modelClass);
  }

	public ValidDataForm(final String id, final HibernateObjectModel<T> model) {
		super(id, model);
	}


	/**
	 * Instantiates this form with a persistent object of the given class and id.
	 * @param id Wicket id
	 * @param modelClass for the persistent object
	 * @param persistentObjectId id of the persistent object
	 */
	public ValidDataForm(final String id, final Class<T> modelClass, final Serializable persistentObjectId) {
		super(id, modelClass, persistentObjectId);
	}


	/**
	 * Form that is nested below a component with a compound model containing a Hibernate
	 * model.
	 * @param id wicket:id
	 */
	public ValidDataForm(final String id) {
		super(id);
	}


	protected void validateModelObject() {
		final T o = getPersistentObjectModel().getObject();
		// TODO [migration]: don't buildDefaultValidatorFactory on every validate; see wicket-bean-validation project -> DefaultValidatorProvider  
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator v = validator == null ? factory.getValidator() : validator;
		Set<ConstraintViolation<T>> constraintViolations = v.validate(o);
		for (ConstraintViolation<T> cv : constraintViolations) {
			error(cv.getMessage());
		}
	}
	

	/**
	 * Add a validator to any form components that have no existing validator
	 * and whose model is recognized by {@link DatabinderValidator#addTo(FormComponent)}.
	 */
	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
		visitFormComponents(new IVisitor<FormComponent<?>, Void>() {

			public void component(FormComponent<?> formComponent, IVisit<Void> visit) {
				if (formComponent.getValidators().isEmpty()) {
					try {
						DatabinderValidator.addTo(formComponent, validator);
					} catch (final UnrecognizedModelException e) {
						// empty
					}
				}
			}
		});
	}
	
	/**
	 * @return dummy validator that can be used to exempt a component
	 * from this form's inspection in {@link #onBeforeRender()}
	 */
	public static IValidator<?> nonValidator() {
		return new IValidator<Object>() {
      private static final long serialVersionUID = 1L;

      public void validate(final IValidatable<Object> validatable) { }
		};
	}
}
