package com.dic.xsuper.dom.properties.cssunit;

/**
 * Unidades CSS suportadas pelo motor Xplorer.
 * Baseado na especificação CSS Values and Units Module Level 3.
 */
public enum CssUnit {
    // --- Unidades absolutas ---
    PX,     // pixels (1px = 1/96 polegada)
    PT,     // pontos (1pt = 1/72 polegada)
    PC,     // picas (1pc = 12pt)
    IN,     // polegadas (1in = 96px)
    CM,     // centímetros (1cm = 37.8px)
    MM,     // milímetros (1mm = 3.78px)
    Q,      // quartos de milímetro (1Q = 0.25mm)

    // --- Unidades relativas à fonte ---
    EM,     // relativo ao font-size do elemento
    REM,    // relativo ao font-size da raiz (html)
    EX,     // altura da letra 'x' do elemento
    CH,     // largura do caractere '0' do elemento

    // --- Unidades relativas à viewport ---
    VW,     // 1% da largura da viewport
    VH,     // 1% da altura da viewport
    VMIN,   // 1% do menor lado da viewport
    VMAX,   // 1% do maior lado da viewport

    // --- Unidades especiais ---
    PERCENT, // % (relativo ao pai, contexto-dependente)
    AUTO,    // valor automático (resolvido pelo layout)
    NONE,    // sem unidade (ex: line-height: 1.5)
    FR;      // fração (para grids/flex, contexto-dependente)
}