package com.hotelbooking.identity.api;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.identity.application.query.GetStaffHotelHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentStaffResolver implements HandlerMethodArgumentResolver {
    private final GetStaffHotelHandler staffHotel;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CurrentStaff.class;
    }

    @Override
    public CurrentStaff resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
        NativeWebRequest request, WebDataBinderFactory binder) {
        var user = request.getUserPrincipal();
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Staff authentication required");
        }
        return new CurrentStaff(user.getName(), staffHotel.handle(new GetStaffHotelHandler.Input(user.getName())));
    }
}
