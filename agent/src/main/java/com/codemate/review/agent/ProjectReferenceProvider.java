package com.codemate.review.agent;

import com.codemate.review.core.model.ChangedMethod;
import com.codemate.review.core.model.PRContext;

@FunctionalInterface
public interface ProjectReferenceProvider {
    String referencesFor(ChangedMethod method, PRContext ctx);
}
