package com.dic.xsuper.lang.ui.properties.style;

import java.util.HashMap;
import java.util.Map;

/**
 * Armazena os estilos computados (finais) de um elemento DOM.
 * Estes valores são o resultado da cascata CSS + herança + valores inline.
 */
public class ComputedStyles {

    // Mapa interno que guarda os valores computados (ex: "color" → "#FFFFFF")
    private final Map<String, String> styles = new HashMap<>();

    /**
     * Define um valor computado para uma propriedade.
     * @param property Nome da propriedade CSS (ex: "color")
     * @param value Valor computado (ex: "#FFFFFF")
     */
    public void set(String property, String value) {
        if (property != null && value != null) {
            styles.put(property.toLowerCase().trim(), value);
        }
    }

    /**
     * Obtém o valor computado de uma propriedade.
     * @param property Nome da propriedade CSS (ex: "color")
     * @return O valor computado, ou null se não definido.
     */
    public String get(String property) {
        if (property == null) return null;
        return styles.get(property.toLowerCase().trim());
    }

    /**
     * Obtém o valor computado de uma propriedade com um valor padrão (fallback).
     * @param property Nome da propriedade CSS
     * @param defaultValue Valor a retornar se a propriedade não estiver definida
     * @return O valor computado ou o valor padrão
     */
    public String getOrDefault(String property, String defaultValue) {
        String value = get(property);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Remove uma propriedade computada.
     * @param property Nome da propriedade CSS
     */
    public void remove(String property) {
        if (property != null) {
            styles.remove(property.toLowerCase().trim());
        }
    }

    /**
     * Limpa todos os estilos computados.
     */
    public void clear() {
        styles.clear();
    }

    /**
     * Verifica se uma propriedade está definida.
     * @param property Nome da propriedade CSS
     * @return true se a propriedade existir
     */
    public boolean has(String property) {
        if (property == null) return false;
        return styles.containsKey(property.toLowerCase().trim());
    }

    /**
     * Retorna uma cópia imutável de todos os estilos computados.
     * @return Mapa não modificável.
     */
    public Map<String, String> getAll() {
        return new HashMap<>(styles);
    }

    /**
     * Funde (merge) outro mapa de estilos com este, sobrepondo os valores.
     * @param other Outro mapa de estilos (ex: do elemento pai para herança)
     */
    public void merge(Map<String, String> other) {
        if (other != null) {
            // Os valores do mapa "other" sobrescrevem os existentes
            // (mas só se a chave já existir ou se quisermos adicionar novos)
            // Normalmente, na herança, queremos adicionar os que faltam,
            // mas não sobrescrever os já definidos.
            for (Map.Entry<String, String> entry : other.entrySet()) {
                styles.putIfAbsent(entry.getKey().toLowerCase().trim(), entry.getValue());
            }
        }
    }

    /**
     * Funde (merge) com prioridade: os estilos deste mapa têm prioridade.
     * @param other Outro mapa de estilos (ex: do elemento pai)
     * @param override Se true, sobrescreve os valores existentes; se false, mantém os existentes.
     */
    public void merge(Map<String, String> other, boolean override) {
        if (other != null) {
            for (Map.Entry<String, String> entry : other.entrySet()) {
                String key = entry.getKey().toLowerCase().trim();
                if (override || !styles.containsKey(key)) {
                    styles.put(key, entry.getValue());
                }
            }
        }
    }

    @Override
    public String toString() {
        return "ComputedStyles{" + styles + "}";
    }
}