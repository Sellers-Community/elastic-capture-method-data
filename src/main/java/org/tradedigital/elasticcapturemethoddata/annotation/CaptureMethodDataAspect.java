package org.tradedigital.elasticcapturemethoddata.annotation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;

@Aspect
@Component
public class CaptureMethodDataAspect {

	public static final String METHOD_PARAMETERS = "parameters_";
	public static final String METHOD_RESPONSE = "response";

	@AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ExceptionHandler)", returning = "returnValue")
	public void sendException(JoinPoint joinPoint, Object returnValue) {
		if(returnValue instanceof ResponseEntity) {
			sendMethodResponse(returnValue, null);
		}
	}
	
	@AfterReturning(pointcut = "@annotation(captureMethodData)", returning = "returnValue")
	public void sendMethodData(JoinPoint joinPoint, Object returnValue, ElasticCaptureMethodData captureMethodData) {
		CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
		
		if (isDataInconsistent(returnValue, captureMethodData, codeSignature)) {
			return;
		}

		Span span = generateSpan(returnValue, codeSignature);

		if (captureMethodData.parameters()) {
			sendMethodParameters(joinPoint, codeSignature, span);
		}

		if (captureMethodData.response() && returnValue != null) {
			sendMethodResponse(returnValue, span);
		}

		if (span != null) {
			span.end();
		}

	}

	private boolean isDataInconsistent(Object returnValue, ElasticCaptureMethodData captureMethodData,
			CodeSignature codeSignature) {
		boolean isSendingNothing = !captureMethodData.parameters() && !captureMethodData.response();
		boolean isInconsistentResponseMode = returnValue == null
				&& !captureMethodData.parameters() && captureMethodData.response();
		boolean isInconsistentParamsMode = codeSignature.getParameterNames().length == 0
				&& captureMethodData.parameters() && !captureMethodData.response();
		boolean isInconsistentAllMode = returnValue == null && codeSignature.getParameterNames().length == 0
				&& captureMethodData.parameters() && captureMethodData.response();
		
		return isSendingNothing || isInconsistentResponseMode || isInconsistentParamsMode || isInconsistentAllMode;
	}

	private Span generateSpan(Object returnValue, CodeSignature codeSignature) {
		Span span = null;

		if (!(returnValue instanceof ResponseEntity)) {
			span = ElasticApm.currentTransaction().startSpan();
			span.setName(codeSignature.getDeclaringTypeName() + "#" + codeSignature.getName());
		}

		return span;
	}

	private void sendMethodParameters(JoinPoint joinPoint, CodeSignature codeSignature, Span span) {
		for (int i = 0; i < codeSignature.getParameterNames().length; i++) {
			if (span == null) {
				ElasticApm.currentTransaction().addCustomContext(
						METHOD_PARAMETERS + codeSignature.getParameterNames()[i],
						new Gson().toJson(joinPoint.getArgs()[i]));
			} else {
				span.addLabel(METHOD_PARAMETERS + codeSignature.getParameterNames()[i],
						new Gson().toJson(joinPoint.getArgs()[i]));
			}
		}
	}

	private void sendMethodResponse(Object returnValue, Span span) {
		if (span == null) {
			ElasticApm.currentTransaction().addCustomContext(METHOD_RESPONSE,
					new Gson().toJson(((ResponseEntity) returnValue).getBody()));
		} else {
			span.addLabel(METHOD_RESPONSE, new Gson().toJson(returnValue));
		}
	}
}
