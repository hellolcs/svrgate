package com.nicednb.svrgate.exception;

import org.springframework.security.core.AuthenticationException;

public class IpAddressRestrictionException extends AuthenticationException {
    public IpAddressRestrictionException(String msg) {
        super(msg);
    }
}