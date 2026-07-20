package com.dic.xsuper.lang.lint;

public class LintConfig {
    public boolean checkUnusedVariables = true;
    public boolean checkUnusedFunctions = true;
    public boolean checkUnusedParameters = true;
    public boolean checkConstantNaming = true;       // const → UPPER_SNAKE_CASE
    public boolean checkBreakContinueOutsideLoop = true;
    public boolean checkReturnOutsideFunction = true;
    public boolean checkVariableShadowing = true;
    public boolean checkDuplicateDeclarations = true;
    public boolean checkPrivateAccess = true;
    public boolean checkTypeCompatibility = true;
    public boolean checkReturnType = true;
    public boolean checkFunctionArity = true;

    // Personalização de severidades (opcional)
    public SeverityOverride overrides = new SeverityOverride();

    public static class SeverityOverride {
        public LintIssue.Severity unusedVariable = LintIssue.Severity.WARNING;
        public LintIssue.Severity unusedFunction = LintIssue.Severity.WARNING;
        public LintIssue.Severity unusedParameter = LintIssue.Severity.WARNING;
        public LintIssue.Severity constantNaming = LintIssue.Severity.SUGGESTION;
        public LintIssue.Severity breakContinueOutsideLoop = LintIssue.Severity.ERROR;
        public LintIssue.Severity returnOutsideFunction = LintIssue.Severity.ERROR;
        public LintIssue.Severity variableShadowing = LintIssue.Severity.WARNING;
        public LintIssue.Severity duplicateDeclaration = LintIssue.Severity.ERROR;
        public LintIssue.Severity privateAccess = LintIssue.Severity.ERROR;
        public LintIssue.Severity typeCompatibility = LintIssue.Severity.ERROR;
        public LintIssue.Severity returnType = LintIssue.Severity.ERROR;
        public LintIssue.Severity functionArity = LintIssue.Severity.ERROR;
    }
}