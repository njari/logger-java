// © 2016-2018 Resurface Labs LLC

package io.resurface.tests;

import io.resurface.HttpRule;
import io.resurface.HttpRules;
import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static com.mscharhag.oleaster.matcher.Matchers.expect;

/**
 * Tests against parser and utilities for HTTP logger rules.
 */
public class HttpRulesTest {

    private void parse_fail(String line) {
        try {
            HttpRules.parseRule(line);
        } catch (IllegalArgumentException iae) {
            return;
        }
        expect(false).toBeTrue();
    }

    private void parse_ok(String line, String verb, String scope, Object param1, Object param2) {
        HttpRule rule = HttpRules.parseRule(line);
        expect(rule.verb).toEqual(verb);

        if (rule.scope == null) {
            expect(scope).toBeNull();
        } else {
            expect(rule.scope.pattern()).toEqual(scope);
        }

        if (rule.param1 == null) {
            expect(param1).toBeNull();
        } else if (rule.param1 instanceof Pattern) {
            expect(param1).toEqual(((Pattern) (rule.param1)).pattern());
        } else {
            expect(param1).toEqual(rule.param1);
        }

        if (rule.param2 == null) {
            expect(param2).toBeNull();
        } else if (rule.param2 instanceof Pattern) {
            expect(param2).toEqual(((Pattern) (rule.param2)).pattern());
        } else {
            expect(param2).toEqual(rule.param2);
        }
    }

    @Test
    public void parsesEmptyRulesTest() {
        expect(HttpRules.parse(null)).toBeEmpty();
        expect(HttpRules.parse("")).toBeEmpty();
        expect(HttpRules.parse(" ")).toBeEmpty();
        expect(HttpRules.parse("\t")).toBeEmpty();
        expect(HttpRules.parse("\n")).toBeEmpty();

        expect(HttpRules.parseRule(null)).toBeNull();
        expect(HttpRules.parseRule("")).toBeNull();
        expect(HttpRules.parseRule(" ")).toBeNull();
        expect(HttpRules.parseRule("\t")).toBeNull();
        expect(HttpRules.parseRule("\n")).toBeNull();
    }

    @Test
    public void parsesRulesWithBadVerbsTest() {
        for (String verb : new String[]{"b", "bozo", "*", ".*"}) {
            parse_fail(verb);
            parse_fail("!.*! " + verb);
            parse_fail("/.*/ " + verb);
            parse_fail("%request_body% " + verb);
            parse_fail("/^request_header:.*/ " + verb);
        }
    }

    @Test
    public void parsesRulesWithInvalidScopesTest() {
        for (String s : new String[]{"request_body", "*", ".*"}) {
            parse_fail("/" + s);
            parse_fail("/" + s + " 1");
            parse_fail("/" + s + " # 1");
            parse_fail("/" + s + "/");
            parse_fail("/" + s + "/ # 1");
            parse_fail(" / " + s);
            parse_fail("// " + s);
            parse_fail("/// " + s);
            parse_fail("/* " + s);
            parse_fail("/? " + s);
            parse_fail("/+ " + s);
            parse_fail("/( " + s);
            parse_fail("/(.* " + s);
            parse_fail("/(.*)) " + s);

            parse_fail("~" + s);
            parse_fail("!" + s + " 1");
            parse_fail("|" + s + " # 1");
            parse_fail("|" + s + "|");
            parse_fail("%" + s + "% # 1");
            parse_fail(" % " + s);
            parse_fail("%% " + s);
            parse_fail("%%% " + s);
            parse_fail("%* " + s);
            parse_fail("%? " + s);
            parse_fail("%+ " + s);
            parse_fail("%( " + s);
            parse_fail("%(.* " + s);
            parse_fail("%(.*)) " + s);

            parse_fail("~" + s + "%");
            parse_fail("!" + s + "%# 1");
            parse_fail("|" + s + "% # 1");
            parse_fail("|" + s + "%");
            parse_fail("%" + s + "| # 1");
            parse_fail("~(.*! " + s);
            parse_fail("~(.*))! " + s);
            parse_fail("/(.*! " + s);
            parse_fail("/(.*))! " + s);
        }
    }

    @Test
    public void parsesAllowHttpRulesTest() {
        parse_fail("allow_http_url whaa");
        parse_ok("allow_http_url", "allow_http_url", null, null, null);
        parse_ok("allow_http_url # be safe bro!", "allow_http_url", null, null, null);
    }

    @Test
    public void parsesCopySessionFieldRulesTest() {
        // with extra params
        parse_fail("|.*| copy_session_field %1%, %2%");
        parse_fail("!.*! copy_session_field /1/, 2");
        parse_fail("/.*/ copy_session_field /1/, /2");
        parse_fail("/.*/ copy_session_field /1/, /2/");
        parse_fail("/.*/ copy_session_field /1/, /2/, /3/ # blah");
        parse_fail("!.*! copy_session_field %1%, %2%, %3%");
        parse_fail("/.*/ copy_session_field /1/, /2/, 3");
        parse_fail("/.*/ copy_session_field /1/, /2/, /3");
        parse_fail("/.*/ copy_session_field /1/, /2/, /3/");
        parse_fail("%.*% copy_session_field /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! copy_session_field");
        parse_fail("/.*/ copy_session_field");
        parse_fail("/.*/ copy_session_field /");
        parse_fail("/.*/ copy_session_field //");
        parse_fail("/.*/ copy_session_field blah");
        parse_fail("/.*/ copy_session_field # bleep");
        parse_fail("/.*/ copy_session_field blah # bleep");

        // with invalid params
        parse_fail("/.*/ copy_session_field /");
        parse_fail("/.*/ copy_session_field //");
        parse_fail("/.*/ copy_session_field ///");
        parse_fail("/.*/ copy_session_field /*/");
        parse_fail("/.*/ copy_session_field /?/");
        parse_fail("/.*/ copy_session_field /+/");
        parse_fail("/.*/ copy_session_field /(/");
        parse_fail("/.*/ copy_session_field /(.*/");
        parse_fail("/.*/ copy_session_field /(.*))/");

        // with valid regexes
        parse_ok("copy_session_field !.*!", "copy_session_field", null, "^.*$", null);
        parse_ok("copy_session_field /.*/", "copy_session_field", null, "^.*$", null);
        parse_ok("copy_session_field /^.*/", "copy_session_field", null, "^.*$", null);
        parse_ok("copy_session_field /.*$/", "copy_session_field", null, "^.*$", null);
        parse_ok("copy_session_field /^.*$/", "copy_session_field", null, "^.*$", null);

        // with valid regexes and escape sequences
        parse_ok("copy_session_field !A\\!|B!", "copy_session_field", null, "^A!|B$", null);
        parse_ok("copy_session_field |A\\|B|", "copy_session_field", null, "^A|B$", null);
        parse_ok("copy_session_field |A\\|B\\|C|", "copy_session_field", null, "^A|B|C$", null);
        parse_ok("copy_session_field /A\\/B\\/C/", "copy_session_field", null, "^A/B/C$", null);
    }

    @Test
    public void parsesRemoveRulesTest() {
        // with extra params
        parse_fail("|.*| remove %1%");
        parse_fail("~.*~ remove 1");
        parse_fail("/.*/ remove /1/");
        parse_fail("/.*/ remove 1 # bleep");
        parse_fail("|.*| remove %1%, %2%");
        parse_fail("!.*! remove /1/, 2");
        parse_fail("/.*/ remove /1/, /2");
        parse_fail("/.*/ remove /1/, /2/");
        parse_fail("/.*/ remove /1/, /2/, /3/ # blah");
        parse_fail("!.*! remove %1%, %2%, %3%");
        parse_fail("/.*/ remove /1/, /2/, 3");
        parse_fail("/.*/ remove /1/, /2/, /3");
        parse_fail("/.*/ remove /1/, /2/, /3/");
        parse_fail("%.*% remove /1/, /2/, /3/ # blah");

        // with valid regexes
        parse_ok("%request_header:cookie|response_header:set-cookie% remove",
                "remove", "^request_header:cookie|response_header:set-cookie$", null, null);
        parse_ok("/request_header:cookie|response_header:set-cookie/ remove",
                "remove", "^request_header:cookie|response_header:set-cookie$", null, null);

        // with valid regexes and escape sequences
        parse_ok("!request_header\\!|response_header:set-cookie! remove",
                "remove", "^request_header!|response_header:set-cookie$", null, null);
        parse_ok("|request_header:cookie\\|response_header:set-cookie| remove",
                "remove", "^request_header:cookie|response_header:set-cookie$", null, null);
        parse_ok("|request_header:cookie\\|response_header:set-cookie\\|boo| remove",
                "remove", "^request_header:cookie|response_header:set-cookie|boo$", null, null);
        parse_ok("/request_header:cookie\\/response_header:set-cookie\\/boo/ remove",
                "remove", "^request_header:cookie/response_header:set-cookie/boo$", null, null);
    }

    @Test
    public void parsesRemoveIfRulesTest() {
        // with extra params
        parse_fail("|.*| remove_if %1%, %2%");
        parse_fail("!.*! remove_if /1/, 2");
        parse_fail("/.*/ remove_if /1/, /2");
        parse_fail("/.*/ remove_if /1/, /2/");
        parse_fail("/.*/ remove_if /1/, /2/, /3/ # blah");
        parse_fail("!.*! remove_if %1%, %2%, %3%");
        parse_fail("/.*/ remove_if /1/, /2/, 3");
        parse_fail("/.*/ remove_if /1/, /2/, /3");
        parse_fail("/.*/ remove_if /1/, /2/, /3/");
        parse_fail("%.*% remove_if /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! remove_if");
        parse_fail("/.*/ remove_if");
        parse_fail("/.*/ remove_if /");
        parse_fail("/.*/ remove_if //");
        parse_fail("/.*/ remove_if blah");
        parse_fail("/.*/ remove_if # bleep");
        parse_fail("/.*/ remove_if blah # bleep");

        // with invalid params
        parse_fail("/.*/ remove_if /");
        parse_fail("/.*/ remove_if //");
        parse_fail("/.*/ remove_if ///");
        parse_fail("/.*/ remove_if /*/");
        parse_fail("/.*/ remove_if /?/");
        parse_fail("/.*/ remove_if /+/");
        parse_fail("/.*/ remove_if /(/");
        parse_fail("/.*/ remove_if /(.*/");
        parse_fail("/.*/ remove_if /(.*))/");

        // with valid regexes
        parse_ok("%response_body% remove_if %<!--SKIP_BODY_LOGGING-->%",
                "remove_if", "^response_body$", "^<!--SKIP_BODY_LOGGING-->$", null);
        parse_ok("/response_body/ remove_if /<!--SKIP_BODY_LOGGING-->/",
                "remove_if", "^response_body$", "^<!--SKIP_BODY_LOGGING-->$", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! remove_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "remove_if", "^request_body|response_body$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("|request_body\\|response_body| remove_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "remove_if", "^request_body|response_body$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("|request_body\\|response_body\\|boo| remove_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|asdf|",
                "remove_if", "^request_body|response_body|boo$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->|asdf$", null);
        parse_ok("/request_body\\/response_body\\/boo/ remove_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|asdf|",
                "remove_if", "^request_body/response_body/boo$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->|asdf$", null);
    }

    @Test
    public void parsesRemoveIfFoundRulesTest() {
        // with extra params
        parse_fail("|.*| remove_if_found %1%, %2%");
        parse_fail("!.*! remove_if_found /1/, 2");
        parse_fail("/.*/ remove_if_found /1/, /2");
        parse_fail("/.*/ remove_if_found /1/, /2/");
        parse_fail("/.*/ remove_if_found /1/, /2/, /3/ # blah");
        parse_fail("!.*! remove_if_found %1%, %2%, %3%");
        parse_fail("/.*/ remove_if_found /1/, /2/, 3");
        parse_fail("/.*/ remove_if_found /1/, /2/, /3");
        parse_fail("/.*/ remove_if_found /1/, /2/, /3/");
        parse_fail("%.*% remove_if_found /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! remove_if_found");
        parse_fail("/.*/ remove_if_found");
        parse_fail("/.*/ remove_if_found /");
        parse_fail("/.*/ remove_if_found //");
        parse_fail("/.*/ remove_if_found blah");
        parse_fail("/.*/ remove_if_found # bleep");
        parse_fail("/.*/ remove_if_found blah # bleep");

        // with invalid params
        parse_fail("/.*/ remove_if_found /");
        parse_fail("/.*/ remove_if_found //");
        parse_fail("/.*/ remove_if_found ///");
        parse_fail("/.*/ remove_if_found /*/");
        parse_fail("/.*/ remove_if_found /?/");
        parse_fail("/.*/ remove_if_found /+/");
        parse_fail("/.*/ remove_if_found /(/");
        parse_fail("/.*/ remove_if_found /(.*/");
        parse_fail("/.*/ remove_if_found /(.*))/");

        // with valid regexes
        parse_ok("%response_body% remove_if_found %<!--SKIP_BODY_LOGGING-->%",
                "remove_if_found", "^response_body$", "<!--SKIP_BODY_LOGGING-->", null);
        parse_ok("/response_body/ remove_if_found /<!--SKIP_BODY_LOGGING-->/",
                "remove_if_found", "^response_body$", "<!--SKIP_BODY_LOGGING-->", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! remove_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "remove_if_found", "^request_body|response_body$", "<!--IGNORE_LOGGING-->|<!-SKIP-->", null);
        parse_ok("|request_body\\|response_body| remove_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "remove_if_found", "^request_body|response_body$", "<!--IGNORE_LOGGING-->|<!-SKIP-->", null);
        parse_ok("|request_body\\|response_body\\|boo| remove_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|asdf|",
                "remove_if_found", "^request_body|response_body|boo$", "<!--IGNORE_LOGGING-->|<!-SKIP-->|asdf", null);
        parse_ok("/request_body\\/response_body\\/boo/ remove_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|asdf|",
                "remove_if_found", "^request_body/response_body/boo$", "<!--IGNORE_LOGGING-->|<!-SKIP-->|asdf", null);
    }

    @Test
    public void parsesRemoveUnlessRulesTest() {
        // with extra params
        parse_fail("|.*| remove_unless %1%, %2%");
        parse_fail("!.*! remove_unless /1/, 2");
        parse_fail("/.*/ remove_unless /1/, /2");
        parse_fail("/.*/ remove_unless /1/, /2/");
        parse_fail("/.*/ remove_unless /1/, /2/, /3/ # blah");
        parse_fail("!.*! remove_unless %1%, %2%, %3%");
        parse_fail("/.*/ remove_unless /1/, /2/, 3");
        parse_fail("/.*/ remove_unless /1/, /2/, /3");
        parse_fail("/.*/ remove_unless /1/, /2/, /3/");
        parse_fail("%.*% remove_unless /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! remove_unless");
        parse_fail("/.*/ remove_unless");
        parse_fail("/.*/ remove_unless /");
        parse_fail("/.*/ remove_unless //");
        parse_fail("/.*/ remove_unless blah");
        parse_fail("/.*/ remove_unless # bleep");
        parse_fail("/.*/ remove_unless blah # bleep");

        // with invalid params
        parse_fail("/.*/ remove_unless /");
        parse_fail("/.*/ remove_unless //");
        parse_fail("/.*/ remove_unless ///");
        parse_fail("/.*/ remove_unless /*/");
        parse_fail("/.*/ remove_unless /?/");
        parse_fail("/.*/ remove_unless /+/");
        parse_fail("/.*/ remove_unless /(/");
        parse_fail("/.*/ remove_unless /(.*/");
        parse_fail("/.*/ remove_unless /(.*))/");

        // with valid regexes
        parse_ok("%response_body% remove_unless %<!--PERFORM_BODY_LOGGING-->%",
                "remove_unless", "^response_body$", "^<!--PERFORM_BODY_LOGGING-->$", null);
        parse_ok("/response_body/ remove_unless /<!--PERFORM_BODY_LOGGING-->/",
                "remove_unless", "^response_body$", "^<!--PERFORM_BODY_LOGGING-->$", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! remove_unless |<!--PERFORM_LOGGING-->\\|<!-SKIP-->|",
                "remove_unless", "^request_body|response_body$", "^<!--PERFORM_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("|request_body\\|response_body| remove_unless |<!--PERFORM_LOGGING-->\\|<!-SKIP-->|",
                "remove_unless", "^request_body|response_body$", "^<!--PERFORM_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("|request_body\\|response_body\\|boo| remove_unless |<!--PERFORM_LOGGING-->\\|<!-SKIP-->\\|skipit|",
                "remove_unless", "^request_body|response_body|boo$", "^<!--PERFORM_LOGGING-->|<!-SKIP-->|skipit$", null);
        parse_ok("/request_body\\/response_body\\/boo/ remove_unless |<!--PERFORM_LOGGING-->\\|<!-SKIP-->\\|skipit|",
                "remove_unless", "^request_body/response_body/boo$", "^<!--PERFORM_LOGGING-->|<!-SKIP-->|skipit$", null);
    }

    @Test
    public void parsesRemoveUnlessFoundRulesTest() {
        // with extra params
        parse_fail("|.*| remove_unless_found %1%, %2%");
        parse_fail("!.*! remove_unless_found /1/, 2");
        parse_fail("/.*/ remove_unless_found /1/, /2");
        parse_fail("/.*/ remove_unless_found /1/, /2/");
        parse_fail("/.*/ remove_unless_found /1/, /2/, /3/ # blah");
        parse_fail("!.*! remove_unless_found %1%, %2%, %3%");
        parse_fail("/.*/ remove_unless_found /1/, /2/, 3");
        parse_fail("/.*/ remove_unless_found /1/, /2/, /3");
        parse_fail("/.*/ remove_unless_found /1/, /2/, /3/");
        parse_fail("%.*% remove_unless_found /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! remove_unless_found");
        parse_fail("/.*/ remove_unless_found");
        parse_fail("/.*/ remove_unless_found /");
        parse_fail("/.*/ remove_unless_found //");
        parse_fail("/.*/ remove_unless_found blah");
        parse_fail("/.*/ remove_unless_found # bleep");
        parse_fail("/.*/ remove_unless_found blah # bleep");

        // with invalid params
        parse_fail("/.*/ remove_unless_found /");
        parse_fail("/.*/ remove_unless_found //");
        parse_fail("/.*/ remove_unless_found ///");
        parse_fail("/.*/ remove_unless_found /*/");
        parse_fail("/.*/ remove_unless_found /?/");
        parse_fail("/.*/ remove_unless_found /+/");
        parse_fail("/.*/ remove_unless_found /(/");
        parse_fail("/.*/ remove_unless_found /(.*/");
        parse_fail("/.*/ remove_unless_found /(.*))/");

        // with valid regexes
        parse_ok("%response_body% remove_unless_found %<!--PERFORM_BODY_LOGGING-->%",
                "remove_unless_found", "^response_body$", "<!--PERFORM_BODY_LOGGING-->", null);
        parse_ok("/response_body/ remove_unless_found /<!--PERFORM_BODY_LOGGING-->/",
                "remove_unless_found", "^response_body$", "<!--PERFORM_BODY_LOGGING-->", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! remove_unless_found |<!--PERFORM_LOGGING-->\\|<!-SKIP-->|",
                "remove_unless_found", "^request_body|response_body$", "<!--PERFORM_LOGGING-->|<!-SKIP-->", null);
        parse_ok("|request_body\\|response_body| remove_unless_found |<!--PERFORM_LOGGING-->\\|<!-SKIP-->|",
                "remove_unless_found", "^request_body|response_body$", "<!--PERFORM_LOGGING-->|<!-SKIP-->", null);
        parse_ok("|request_body\\|response_body\\|boo| remove_unless_found |<!--PERFORM_LOGGING-->\\|<!-SKIP-->\\|skipit|",
                "remove_unless_found", "^request_body|response_body|boo$", "<!--PERFORM_LOGGING-->|<!-SKIP-->|skipit", null);
        parse_ok("/request_body\\/response_body\\/boo/ remove_unless_found |<!--PERFORM_LOGGING-->\\|<!-SKIP-->\\|skipit|",
                "remove_unless_found", "^request_body/response_body/boo$", "<!--PERFORM_LOGGING-->|<!-SKIP-->|skipit", null);
    }

    @Test
    public void parsesReplaceRulesTest() {
        // with extra params
        parse_fail("!.*! replace %1%, %2%, %3%");
        parse_fail("/.*/ replace /1/, /2/, 3");
        parse_fail("/.*/ replace /1/, /2/, /3");
        parse_fail("/.*/ replace /1/, /2/, /3/");
        parse_fail("%.*% replace /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! replace");
        parse_fail("/.*/ replace");
        parse_fail("/.*/ replace /");
        parse_fail("/.*/ replace //");
        parse_fail("/.*/ replace blah");
        parse_fail("/.*/ replace # bleep");
        parse_fail("/.*/ replace blah # bleep");
        parse_fail("!.*! replace boo yah");
        parse_fail("/.*/ replace boo yah");
        parse_fail("/.*/ replace boo yah # bro");
        parse_fail("/.*/ replace /.*/ # bleep");
        parse_fail("/.*/ replace /.*/, # bleep");
        parse_fail("/.*/ replace /.*/, /# bleep");
        parse_fail("/.*/ replace // # bleep");
        parse_fail("/.*/ replace // // # bleep");

        // with invalid params
        parse_fail("/.*/ replace /");
        parse_fail("/.*/ replace //");
        parse_fail("/.*/ replace ///");
        parse_fail("/.*/ replace /*/");
        parse_fail("/.*/ replace /?/");
        parse_fail("/.*/ replace /+/");
        parse_fail("/.*/ replace /(/");
        parse_fail("/.*/ replace /(.*/");
        parse_fail("/.*/ replace /(.*))/");
        parse_fail("/.*/ replace /1/, ~");
        parse_fail("/.*/ replace /1/, !");
        parse_fail("/.*/ replace /1/, %");
        parse_fail("/.*/ replace /1/, |");
        parse_fail("/.*/ replace /1/, /");

        // with valid regexes
        parse_ok("%response_body% replace %kurt%, %vagner%", "replace", "^response_body$", "kurt", "vagner");
        parse_ok("/response_body/ replace /kurt/, /vagner/", "replace", "^response_body$", "kurt", "vagner");
        parse_ok("%response_body|.+_header:.+% replace %kurt%, %vagner%",
                "replace", "^response_body|.+_header:.+$", "kurt", "vagner");
        parse_ok("|response_body\\|.+_header:.+| replace |kurt|, |vagner\\|frazier|",
                "replace", "^response_body|.+_header:.+$", "kurt", "vagner|frazier");

        // with valid regexes and escape sequences
        parse_ok("|response_body\\|.+_header:.+| replace |kurt|, |vagner|",
                "replace", "^response_body|.+_header:.+$", "kurt", "vagner");
        parse_ok("|response_body\\|.+_header:.+\\|boo| replace |kurt|, |vagner|",
                "replace", "^response_body|.+_header:.+|boo$", "kurt", "vagner");
        parse_ok("|response_body| replace |kurt\\|bruce|, |vagner|",
                "replace", "^response_body$", "kurt|bruce", "vagner");
        parse_ok("|response_body| replace |kurt\\|bruce\\|kevin|, |vagner|",
                "replace", "^response_body$", "kurt|bruce|kevin", "vagner");
        parse_ok("|response_body| replace /kurt\\/bruce\\/kevin/, |vagner|",
                "replace", "^response_body$", "kurt/bruce/kevin", "vagner");
    }

    @Test
    public void parsesSampleRulesTest() {
        parse_fail("sample");
        parse_fail("sample 50 50");
        parse_fail("sample 0");
        parse_fail("sample 100");
        parse_fail("sample 105");
        parse_fail("sample 10.5");
        parse_fail("sample blue");
        parse_fail("sample # bleep");
        parse_fail("sample blue # bleep");
        parse_fail("sample //");
        parse_fail("sample /42/");
        parse_ok("sample 50", "sample", null, 50, null);
        parse_ok("sample 72 # comment", "sample", null, 72, null);
    }

    @Test
    public void parsesSkipCompressionRulesTest() {
        parse_fail("skip_compression whaa");
        parse_ok("skip_compression", "skip_compression", null, null, null);
        parse_ok("skip_compression # slightly faster!", "skip_compression", null, null, null);
    }

    @Test
    public void parsesSkipSubmissionRulesTest() {
        parse_fail("skip_submission whaa");
        parse_ok("skip_submission", "skip_submission", null, null, null);
        parse_ok("skip_submission # slightly faster!", "skip_submission", null, null, null);
    }

    @Test
    public void parsesStopRulesTest() {
        // with extra params
        parse_fail("|.*| stop %1%");
        parse_fail("~.*~ stop 1");
        parse_fail("/.*/ stop /1/");
        parse_fail("/.*/ stop 1 # bleep");
        parse_fail("|.*| stop %1%, %2%");
        parse_fail("!.*! stop /1/, 2");
        parse_fail("/.*/ stop /1/, /2");
        parse_fail("/.*/ stop /1/, /2/");
        parse_fail("/.*/ stop /1/, /2/, /3/ # blah");
        parse_fail("!.*! stop %1%, %2%, %3%");
        parse_fail("/.*/ stop /1/, /2/, 3");
        parse_fail("/.*/ stop /1/, /2/, /3");
        parse_fail("/.*/ stop /1/, /2/, /3/");
        parse_fail("%.*% stop /1/, /2/, /3/ # blah");

        // with valid regexes
        parse_ok("%request_header:skip_usage_logging% stop", "stop", "^request_header:skip_usage_logging$", null, null);
        parse_ok("|request_header:skip_usage_logging| stop", "stop", "^request_header:skip_usage_logging$", null, null);
        parse_ok("/request_header:skip_usage_logging/ stop", "stop", "^request_header:skip_usage_logging$", null, null);

        // with valid regexes and escape sequences
        parse_ok("!request_header\\!! stop", "stop", "^request_header!$", null, null);
        parse_ok("|request_header\\|response_header| stop", "stop", "^request_header|response_header$", null, null);
        parse_ok("|request_header\\|response_header\\|boo| stop", "stop", "^request_header|response_header|boo$", null, null);
        parse_ok("/request_header\\/response_header\\/boo/ stop", "stop", "^request_header/response_header/boo$", null, null);
    }

    @Test
    public void parsesStopIfRulesTest() {
        // with extra params
        parse_fail("|.*| stop_if %1%, %2%");
        parse_fail("!.*! stop_if /1/, 2");
        parse_fail("/.*/ stop_if /1/, /2");
        parse_fail("/.*/ stop_if /1/, /2/");
        parse_fail("/.*/ stop_if /1/, /2/, /3/ # blah");
        parse_fail("!.*! stop_if %1%, %2%, %3%");
        parse_fail("/.*/ stop_if /1/, /2/, 3");
        parse_fail("/.*/ stop_if /1/, /2/, /3");
        parse_fail("/.*/ stop_if /1/, /2/, /3/");
        parse_fail("%.*% stop_if /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! stop_if");
        parse_fail("/.*/ stop_if");
        parse_fail("/.*/ stop_if /");
        parse_fail("/.*/ stop_if //");
        parse_fail("/.*/ stop_if blah");
        parse_fail("/.*/ stop_if # bleep");
        parse_fail("/.*/ stop_if blah # bleep");

        // with invalid params
        parse_fail("/.*/ stop_if /");
        parse_fail("/.*/ stop_if //");
        parse_fail("/.*/ stop_if ///");
        parse_fail("/.*/ stop_if /*/");
        parse_fail("/.*/ stop_if /?/");
        parse_fail("/.*/ stop_if /+/");
        parse_fail("/.*/ stop_if /(/");
        parse_fail("/.*/ stop_if /(.*/");
        parse_fail("/.*/ stop_if /(.*))/");

        // with valid regexes
        parse_ok("%response_body% stop_if %<!--IGNORE_LOGGING-->%", "stop_if", "^response_body$", "^<!--IGNORE_LOGGING-->$", null);
        parse_ok("/response_body/ stop_if /<!--IGNORE_LOGGING-->/", "stop_if", "^response_body$", "^<!--IGNORE_LOGGING-->$", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! stop_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "stop_if", "^request_body|response_body$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("!request_body|response_body|boo\\!! stop_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "stop_if", "^request_body|response_body|boo!$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("|request_body\\|response_body| stop_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "stop_if", "^request_body|response_body$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->$", null);
        parse_ok("|request_body\\|response_body| stop_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|pipe\\||",
                "stop_if", "^request_body|response_body$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->|pipe|$", null);
        parse_ok("/request_body\\/response_body/ stop_if |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|pipe\\||",
                "stop_if", "^request_body/response_body$", "^<!--IGNORE_LOGGING-->|<!-SKIP-->|pipe|$", null);
    }

    @Test
    public void parsesStopIfFoundRulesTest() {
        // with extra params
        parse_fail("|.*| stop_if_found %1%, %2%");
        parse_fail("!.*! stop_if_found /1/, 2");
        parse_fail("/.*/ stop_if_found /1/, /2");
        parse_fail("/.*/ stop_if_found /1/, /2/");
        parse_fail("/.*/ stop_if_found /1/, /2/, /3/ # blah");
        parse_fail("!.*! stop_if_found %1%, %2%, %3%");
        parse_fail("/.*/ stop_if_found /1/, /2/, 3");
        parse_fail("/.*/ stop_if_found /1/, /2/, /3");
        parse_fail("/.*/ stop_if_found /1/, /2/, /3/");
        parse_fail("%.*% stop_if_found /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! stop_if_found");
        parse_fail("/.*/ stop_if_found");
        parse_fail("/.*/ stop_if_found /");
        parse_fail("/.*/ stop_if_found //");
        parse_fail("/.*/ stop_if_found blah");
        parse_fail("/.*/ stop_if_found # bleep");
        parse_fail("/.*/ stop_if_found blah # bleep");

        // with invalid params
        parse_fail("/.*/ stop_if_found /");
        parse_fail("/.*/ stop_if_found //");
        parse_fail("/.*/ stop_if_found ///");
        parse_fail("/.*/ stop_if_found /*/");
        parse_fail("/.*/ stop_if_found /?/");
        parse_fail("/.*/ stop_if_found /+/");
        parse_fail("/.*/ stop_if_found /(/");
        parse_fail("/.*/ stop_if_found /(.*/");
        parse_fail("/.*/ stop_if_found /(.*))/");

        // with valid regexes
        parse_ok("%response_body% stop_if_found %<!--IGNORE_LOGGING-->%",
                "stop_if_found", "^response_body$", "<!--IGNORE_LOGGING-->", null);
        parse_ok("/response_body/ stop_if_found /<!--IGNORE_LOGGING-->/",
                "stop_if_found", "^response_body$", "<!--IGNORE_LOGGING-->", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! stop_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "stop_if_found", "^request_body|response_body$", "<!--IGNORE_LOGGING-->|<!-SKIP-->", null);
        parse_ok("!request_body|response_body|boo\\!! stop_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "stop_if_found", "^request_body|response_body|boo!$", "<!--IGNORE_LOGGING-->|<!-SKIP-->", null);
        parse_ok("|request_body\\|response_body| stop_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->|",
                "stop_if_found", "^request_body|response_body$", "<!--IGNORE_LOGGING-->|<!-SKIP-->", null);
        parse_ok("|request_body\\|response_body| stop_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|pipe\\||",
                "stop_if_found", "^request_body|response_body$", "<!--IGNORE_LOGGING-->|<!-SKIP-->|pipe|", null);
        parse_ok("/request_body\\/response_body/ stop_if_found |<!--IGNORE_LOGGING-->\\|<!-SKIP-->\\|pipe\\||",
                "stop_if_found", "^request_body/response_body$", "<!--IGNORE_LOGGING-->|<!-SKIP-->|pipe|", null);
    }

    @Test
    public void parsesStopUnlessRulesTest() {
        // with extra params
        parse_fail("|.*| stop_unless %1%, %2%");
        parse_fail("!.*! stop_unless /1/, 2");
        parse_fail("/.*/ stop_unless /1/, /2");
        parse_fail("/.*/ stop_unless /1/, /2/");
        parse_fail("/.*/ stop_unless /1/, /2/, /3/ # blah");
        parse_fail("!.*! stop_unless %1%, %2%, %3%");
        parse_fail("/.*/ stop_unless /1/, /2/, 3");
        parse_fail("/.*/ stop_unless /1/, /2/, /3");
        parse_fail("/.*/ stop_unless /1/, /2/, /3/");
        parse_fail("%.*% stop_unless /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! stop_unless");
        parse_fail("/.*/ stop_unless");
        parse_fail("/.*/ stop_unless /");
        parse_fail("/.*/ stop_unless //");
        parse_fail("/.*/ stop_unless blah");
        parse_fail("/.*/ stop_unless # bleep");
        parse_fail("/.*/ stop_unless blah # bleep");

        // with invalid params
        parse_fail("/.*/ stop_unless /");
        parse_fail("/.*/ stop_unless //");
        parse_fail("/.*/ stop_unless ///");
        parse_fail("/.*/ stop_unless /*/");
        parse_fail("/.*/ stop_unless /?/");
        parse_fail("/.*/ stop_unless /+/");
        parse_fail("/.*/ stop_unless /(/");
        parse_fail("/.*/ stop_unless /(.*/");
        parse_fail("/.*/ stop_unless /(.*))/");

        // with valid regexes
        parse_ok("%response_body% stop_unless %<!--DO_LOGGING-->%", "stop_unless", "^response_body$", "^<!--DO_LOGGING-->$", null);
        parse_ok("/response_body/ stop_unless /<!--DO_LOGGING-->/", "stop_unless", "^response_body$", "^<!--DO_LOGGING-->$", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! stop_unless |<!--DO_LOGGING-->\\|<!-NOSKIP-->|",
                "stop_unless", "^request_body|response_body$", "^<!--DO_LOGGING-->|<!-NOSKIP-->$", null);
        parse_ok("!request_body|response_body|boo\\!! stop_unless |<!--DO_LOGGING-->\\|<!-NOSKIP-->|",
                "stop_unless", "^request_body|response_body|boo!$", "^<!--DO_LOGGING-->|<!-NOSKIP-->$", null);
        parse_ok("|request_body\\|response_body| stop_unless |<!--DO_LOGGING-->\\|<!-NOSKIP-->|",
                "stop_unless", "^request_body|response_body$", "^<!--DO_LOGGING-->|<!-NOSKIP-->$", null);
        parse_ok("|request_body\\|response_body| stop_unless |<!--DO_LOGGING-->\\|<!-NOSKIP-->\\|pipe\\||",
                "stop_unless", "^request_body|response_body$", "^<!--DO_LOGGING-->|<!-NOSKIP-->|pipe|$", null);
        parse_ok("/request_body\\/response_body/ stop_unless |<!--DO_LOGGING-->\\|<!-NOSKIP-->\\|pipe\\||",
                "stop_unless", "^request_body/response_body$", "^<!--DO_LOGGING-->|<!-NOSKIP-->|pipe|$", null);
    }

    @Test
    public void parsesStopUnlessFoundRulesTest() {
        // with extra params
        parse_fail("|.*| stop_unless_found %1%, %2%");
        parse_fail("!.*! stop_unless_found /1/, 2");
        parse_fail("/.*/ stop_unless_found /1/, /2");
        parse_fail("/.*/ stop_unless_found /1/, /2/");
        parse_fail("/.*/ stop_unless_found /1/, /2/, /3/ # blah");
        parse_fail("!.*! stop_unless_found %1%, %2%, %3%");
        parse_fail("/.*/ stop_unless_found /1/, /2/, 3");
        parse_fail("/.*/ stop_unless_found /1/, /2/, /3");
        parse_fail("/.*/ stop_unless_found /1/, /2/, /3/");
        parse_fail("%.*% stop_unless_found /1/, /2/, /3/ # blah");

        // with missing params
        parse_fail("!.*! stop_unless_found");
        parse_fail("/.*/ stop_unless_found");
        parse_fail("/.*/ stop_unless_found /");
        parse_fail("/.*/ stop_unless_found //");
        parse_fail("/.*/ stop_unless_found blah");
        parse_fail("/.*/ stop_unless_found # bleep");
        parse_fail("/.*/ stop_unless_found blah # bleep");

        // with invalid params
        parse_fail("/.*/ stop_unless_found /");
        parse_fail("/.*/ stop_unless_found //");
        parse_fail("/.*/ stop_unless_found ///");
        parse_fail("/.*/ stop_unless_found /*/");
        parse_fail("/.*/ stop_unless_found /?/");
        parse_fail("/.*/ stop_unless_found /+/");
        parse_fail("/.*/ stop_unless_found /(/");
        parse_fail("/.*/ stop_unless_found /(.*/");
        parse_fail("/.*/ stop_unless_found /(.*))/");

        // with valid regexes
        parse_ok("%response_body% stop_unless_found %<!--DO_LOGGING-->%",
                "stop_unless_found", "^response_body$", "<!--DO_LOGGING-->", null);
        parse_ok("/response_body/ stop_unless_found /<!--DO_LOGGING-->/",
                "stop_unless_found", "^response_body$", "<!--DO_LOGGING-->", null);

        // with valid regexes and escape sequences
        parse_ok("!request_body|response_body! stop_unless_found |<!--DO_LOGGING-->\\|<!-NOSKIP-->|",
                "stop_unless_found", "^request_body|response_body$", "<!--DO_LOGGING-->|<!-NOSKIP-->", null);
        parse_ok("!request_body|response_body|boo\\!! stop_unless_found |<!--DO_LOGGING-->\\|<!-NOSKIP-->|",
                "stop_unless_found", "^request_body|response_body|boo!$", "<!--DO_LOGGING-->|<!-NOSKIP-->", null);
        parse_ok("|request_body\\|response_body| stop_unless_found |<!--DO_LOGGING-->\\|<!-NOSKIP-->|",
                "stop_unless_found", "^request_body|response_body$", "<!--DO_LOGGING-->|<!-NOSKIP-->", null);
        parse_ok("|request_body\\|response_body| stop_unless_found |<!--DO_LOGGING-->\\|<!-NOSKIP-->\\|pipe\\||",
                "stop_unless_found", "^request_body|response_body$", "<!--DO_LOGGING-->|<!-NOSKIP-->|pipe|", null);
        parse_ok("/request_body\\/response_body/ stop_unless_found |<!--DO_LOGGING-->\\|<!-NOSKIP-->\\|pipe\\||",
                "stop_unless_found", "^request_body/response_body$", "<!--DO_LOGGING-->|<!-NOSKIP-->|pipe|", null);
    }

    @Test
    public void raisesExpectedErrorsTest() {
        try {
            HttpRules.parseRule("/*! stop");
            expect(false).toBeTrue();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("Invalid expression (/*!) in rule: /*! stop");
        }

        try {
            HttpRules.parseRule("/*/ stop");
            expect(false).toBeTrue();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("Invalid regex (/*/) in rule: /*/ stop");
        }

        try {
            HttpRules.parseRule("/boo");
            expect(false).toBeTrue();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("Invalid rule: /boo");
        }

        try {
            HttpRules.parseRule("sample 123");
            expect(false).toBeTrue();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("Invalid sample percent: 123");
        }

        try {
            HttpRules.parseRule("!!! stop");
            expect(false).toBeTrue();
        } catch (IllegalArgumentException iae) {
            expect(iae.getMessage()).toEqual("Unescaped separator (!) in rule: !!! stop");
        }
    }

    @Test
    public void usesBasicRulesTest() {
        List<HttpRule> rules = HttpRules.parse(HttpRules.getBasicRules());
        expect(rules.size()).toEqual(3);
        expect(rules.stream().filter(r -> "remove".equals(r.verb)).count()).toEqual(1);
        expect(rules.stream().filter(r -> "replace".equals(r.verb)).count()).toEqual(2);

        rules = HttpRules.parse("include basic");
        expect(rules.size()).toEqual(3);
        expect(rules.stream().filter(r -> "remove".equals(r.verb)).count()).toEqual(1);
        expect(rules.stream().filter(r -> "replace".equals(r.verb)).count()).toEqual(2);

        rules = HttpRules.parse("include basic\n");
        expect(rules.size()).toEqual(3);
        expect(rules.stream().filter(r -> "remove".equals(r.verb)).count()).toEqual(1);
        expect(rules.stream().filter(r -> "replace".equals(r.verb)).count()).toEqual(2);

        rules = HttpRules.parse("include basic\nsample 50");
        expect(rules.size()).toEqual(4);
        expect(rules.stream().filter(r -> "sample".equals(r.verb)).count()).toEqual(1);

        rules = HttpRules.parse(" include basic\ninclude basic");
        expect(rules.size()).toEqual(6);
        expect(rules.stream().filter(r -> "remove".equals(r.verb)).count()).toEqual(2);
        expect(rules.stream().filter(r -> "replace".equals(r.verb)).count()).toEqual(4);

        rules = HttpRules.parse("include basic\nsample 50\ninclude basic");
        expect(rules.size()).toEqual(7);
    }

}
