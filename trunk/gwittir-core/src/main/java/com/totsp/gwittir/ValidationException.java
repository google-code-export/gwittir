/*
 * ValidationException.java
 *
 * Created on April 12, 2007, 5:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.totsp.gwittir;


/**
 *
 * @author cooper
 */
public class ValidationException extends Exception {
    
    private Class validatorClass = null;
    /** Creates a new instance of ValidationException */
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Class validatorClass ){
        super(message);
        this.validatorClass = validatorClass;
    }

    public Class getValidatorClass() {
        return validatorClass;
    }
}
