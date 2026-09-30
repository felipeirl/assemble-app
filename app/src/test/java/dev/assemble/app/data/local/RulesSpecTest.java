package dev.assemble.app.data.local;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Teste JVM puro da lógica espelhada das rules (§5) — sem Firebase, sem
 * emulator, sem deploy. Roda via Gradle ou javac/java direto com junit no
 * classpath (ver docs/setup-firebase.md §4).
 */
public class RulesSpecTest {

    private static Set<String> keys(String... ks) {
        return new HashSet<>(Arrays.asList(ks));
    }

    @Test
    public void permiteChoiceValidoDoDono() {
        assertTrue(RulesSpec.canCreateOrUpdateDecision("uid-1", "uid-1", keys("choice"), "PASS"));
        assertTrue(RulesSpec.canCreateOrUpdateDecision("uid-1", "uid-1",
                keys("choice", "updatedAt"), "ASSEMBLE"));
    }

    @Test
    public void negaCadaCampoAutoritativo() {
        for (String field : new String[]{"score", "threshold", "algorithmVersion", "connected"}) {
            assertFalse("write com " + field,
                    RulesSpec.canCreateOrUpdateDecision("uid-1", "uid-1",
                            keys("choice", "updatedAt", field), "ASSEMBLE"));
        }
    }

    @Test
    public void negaChoiceInvalidoOuAusente() {
        assertFalse(RulesSpec.canCreateOrUpdateDecision("uid-1", "uid-1", keys("choice"), "LIKE"));
        assertFalse(RulesSpec.canCreateOrUpdateDecision("uid-1", "uid-1", keys("updatedAt"), null));
        assertFalse(RulesSpec.canCreateOrUpdateDecision("uid-1", "uid-1", keys(), "PASS"));
    }

    @Test
    public void negaOutroUidOuSemAuth() {
        assertFalse(RulesSpec.canCreateOrUpdateDecision("uid-2", "uid-1", keys("choice"), "PASS"));
        assertFalse(RulesSpec.canCreateOrUpdateDecision(null, "uid-1", keys("choice"), "PASS"));
    }

    @Test
    public void negaDeleteDecision() {
        assertFalse(RulesSpec.canDeleteDecision("uid-1", "uid-1"));
    }

    @Test
    public void negaWriteMatchMessageCliente() {
        assertFalse(RulesSpec.canWriteMatchOrMessage("uid-1", "uid-1"));
        assertFalse(RulesSpec.canWriteMatchOrMessage("uid-1", "uid-1"));
    }

    @Test
    public void leituraPropriaOkDeOutroNegada() {
        assertTrue(RulesSpec.canReadOwn("uid-1", "uid-1"));
        assertFalse(RulesSpec.canReadOwn("uid-2", "uid-1"));
        assertTrue(RulesSpec.canReadWriteProfile("uid-1", "uid-1"));
        assertFalse(RulesSpec.canReadWriteProfile("uid-2", "uid-1"));
    }
}
