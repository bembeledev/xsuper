package com.dic.xsuper.render.javafx.event;


/**
 * Mapeia todos os códigos de tecla GLFW para um enum tipado.
 * Baseado na especificação GLFW 3.4.
 *
 * @see <a href="https://www.glfw.org/docs/3.3/group__keys.html">GLFW Key Codes</a>
 */
public enum XplKeyCode  {

    // =====================================================================
    // 1. TECLAS NÃO IMPRIMÍVEIS (Controle)
    // =====================================================================


    KEY_UNKNOWN(-1),

    KEY_SPACE(32),
    KEY_APOSTROPHE(39),
    KEY_COMMA(44),
    KEY_MINUS(45),
    KEY_PERIOD(46),
    KEY_SLASH(47),

    // Números
    KEY_0(48),
    KEY_1(49),
    KEY_2(50),
    KEY_3(51),
    KEY_4(52),
    KEY_5(53),
    KEY_6(54),
    KEY_7(55),
    KEY_8(56),
    KEY_9(57),

    // Letras (maiúsculas e minúsculas são mapeadas para o mesmo código)
    KEY_A(65),
    KEY_B(66),
    KEY_C(67),
    KEY_D(68),
    KEY_E(69),
    KEY_F(70),
    KEY_G(71),
    KEY_H(72),
    KEY_I(73),
    KEY_J(74),
    KEY_K(75),
    KEY_L(76),
    KEY_M(77),
    KEY_N(78),
    KEY_O(79),
    KEY_P(80),
    KEY_Q(81),
    KEY_R(82),
    KEY_S(83),
    KEY_T(84),
    KEY_U(85),
    KEY_V(86),
    KEY_W(87),
    KEY_X(88),
    KEY_Y(89),
    KEY_Z(90),

    // =====================================================================
    // 2. TECLAS DE FUNÇÃO (F1-F25)
    // =====================================================================

    KEY_F1(290),
    KEY_F2(291),
    KEY_F3(292),
    KEY_F4(293),
    KEY_F5(294),
    KEY_F6(295),
    KEY_F7(296),
    KEY_F8(297),
    KEY_F9(298),
    KEY_F10(299),
    KEY_F11(300),
    KEY_F12(301),
    KEY_F13(302),
    KEY_F14(303),
    KEY_F15(304),
    KEY_F16(305),
    KEY_F17(306),
    KEY_F18(307),
    KEY_F19(308),
    KEY_F20(309),
    KEY_F21(310),
    KEY_F22(311),
    KEY_F23(312),
    KEY_F24(313),
    KEY_F25(314), // A partir do GLFW 3.4

    // =====================================================================
    // 3. TECLAS DE NAVEGAÇÃO
    // =====================================================================

    KEY_UP(265),
    KEY_DOWN(264),
    KEY_LEFT(263),
    KEY_RIGHT(262),

    KEY_PAGE_UP(266),
    KEY_PAGE_DOWN(267),
    KEY_HOME(268),
    KEY_END(269),
    KEY_INSERT(260),
    KEY_DELETE(261),

    // =====================================================================
    // 4. TECLAS DE EDIÇÃO
    // =====================================================================

    KEY_BACKSPACE(259),
    KEY_ENTER(257),
    KEY_ESCAPE(256),
    KEY_TAB(258),
    KEY_CAPS_LOCK(280),
    KEY_SCROLL_LOCK(281),
    KEY_NUM_LOCK(282),
    KEY_PRINT_SCREEN(283),
    KEY_PAUSE(284),

    // =====================================================================
    // 5. TECLAS MODIFICADORAS
    // =====================================================================

    KEY_LEFT_SHIFT(340),
    KEY_LEFT_CONTROL(341),
    KEY_LEFT_ALT(342),
    KEY_LEFT_SUPER(343),    // Windows / Command
    KEY_RIGHT_SHIFT(344),
    KEY_RIGHT_CONTROL(345),
    KEY_RIGHT_ALT(346),
    KEY_RIGHT_SUPER(347),
    KEY_MENU(348),          // Tecla de menu (contexto)

    // =====================================================================
    // 6. TECLAS DE SÍMBOLOS E PONTUAÇÃO (internacionais)
    // =====================================================================

    KEY_SEMICOLON(59),
    KEY_EQUAL(61),
    KEY_LEFT_BRACKET(91),
    KEY_BACKSLASH(92),
    KEY_RIGHT_BRACKET(93),
    KEY_GRAVE_ACCENT(96),
    KEY_WORLD_1(161),
    KEY_WORLD_2(162),

    // =====================================================================
    // 7. TECLAS DO TECLADO NUMÉRICO
    // =====================================================================

    KEY_KP_0(320),
    KEY_KP_1(321),
    KEY_KP_2(322),
    KEY_KP_3(323),
    KEY_KP_4(324),
    KEY_KP_5(325),
    KEY_KP_6(326),
    KEY_KP_7(327),
    KEY_KP_8(328),
    KEY_KP_9(329),
    KEY_KP_DECIMAL(330),
    KEY_KP_DIVIDE(331),
    KEY_KP_MULTIPLY(332),
    KEY_KP_SUBTRACT(333),
    KEY_KP_ADD(334),
    KEY_KP_ENTER(335),
    KEY_KP_EQUAL(336),      // A partir do GLFW 3.4

    // =====================================================================
    // 8. TECLAS MULTIMÍDIA (opcional, disponíveis em alguns teclados)
    // =====================================================================

    KEY_MEDIA_PLAY(400),
    KEY_MEDIA_PAUSE(401),
    KEY_MEDIA_STOP(402),
    KEY_MEDIA_NEXT(403),
    KEY_MEDIA_PREVIOUS(404),
    KEY_MEDIA_RECORD(405),
    KEY_MEDIA_REWIND(406),
    KEY_MEDIA_FAST_FORWARD(407),
    KEY_MEDIA_SELECT(408),
    KEY_MEDIA_EJECT(409),
    KEY_MEDIA_SHUFFLE(410),
    KEY_MEDIA_REPEAT(411),

    // =====================================================================
    // 9. TECLAS DE CONTROLE DE ÁUDIO
    // =====================================================================

    KEY_AUDIO_MUTE(412),
    KEY_AUDIO_VOLUME_UP(413),
    KEY_AUDIO_VOLUME_DOWN(414),

    // =====================================================================
    // 10. TECLAS DE NAVEGAÇÃO WEB
    // =====================================================================

    KEY_WEB_HOME(415),
    KEY_WEB_BACK(416),
    KEY_WEB_FORWARD(417),
    KEY_WEB_REFRESH(418),
    KEY_WEB_STOP(419),
    KEY_WEB_SEARCH(420),
    KEY_WEB_FAVORITES(421),

    // =====================================================================
    // 11. TECLAS DE CONTROLE DE APLICAÇÃO
    // =====================================================================

    KEY_APPLICATION(422),
    KEY_MAIL(423),
    KEY_CALCULATOR(424),
    KEY_COMPUTER(425),
    KEY_BRIGHTNESS_UP(426),
    KEY_BRIGHTNESS_DOWN(427),
    KEY_DISPLAY_SWITCH(428),
    KEY_KBD_ILLUMINATION(429),
    KEY_KBD_ILLUMINATION_UP(430),
    KEY_KBD_ILLUMINATION_DOWN(431),

    // =====================================================================
    // 12. SCROLL (mantido para compatibilidade, mas use SCROLL_UP/DOWN)
    // =====================================================================

    SCROLL_UP(1),
    SCROLL_DOWN(-1);

    // =====================================================================
    // ATRIBUTO E CONSTRUTOR
    // =====================================================================

    private final int value;

    XplKeyCode (int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    /**
     * Traduz o número bruto do hardware (GLFW) para o enum tipado.
     */
    public static XplKeyCode  fromIntValue(int code) {
        for (XplKeyCode  key : values()) {
            if (key.value == code) {
                return key;
            }
        }
        return KEY_UNKNOWN;
    }

    /**
     * Verifica se a tecla é uma tecla modificadora (Shift, Ctrl, Alt, Super).
     */
    public boolean isModifier() {
        return this == KEY_LEFT_SHIFT || this == KEY_RIGHT_SHIFT ||
                this == KEY_LEFT_CONTROL || this == KEY_RIGHT_CONTROL ||
                this == KEY_LEFT_ALT || this == KEY_RIGHT_ALT ||
                this == KEY_LEFT_SUPER || this == KEY_RIGHT_SUPER;
    }

    /**
     * Verifica se a tecla é uma tecla de função (F1-F25).
     */
    public boolean isFunctionKey() {
        int v = this.value;
        return v >= 290 && v <= 314;
    }

    /**
     * Verifica se a tecla é uma tecla de navegação (setas, PageUp, etc.)
     */
    public boolean isNavigationKey() {
        return this == KEY_UP || this == KEY_DOWN || this == KEY_LEFT || this == KEY_RIGHT ||
                this == KEY_PAGE_UP || this == KEY_PAGE_DOWN ||
                this == KEY_HOME || this == KEY_END || this == KEY_INSERT || this == KEY_DELETE;
    }

    /**
     * Verifica se a tecla é uma tecla do teclado numérico.
     */
    public boolean isKeypadKey() {
        int v = this.value;
        return v >= 320 && v <= 336;
    }

    /**
     * Verifica se é uma tecla imprimível (letra, número, símbolo, espaço)
     */
    public boolean isPrintable() {
        return (this.value >= 32 && this.value <= 126) || // ASCII imprimível
                this == KEY_SPACE ||
                this == KEY_APOSTROPHE || this == KEY_COMMA || this == KEY_MINUS ||
                this == KEY_PERIOD || this == KEY_SLASH || this == KEY_SEMICOLON ||
                this == KEY_EQUAL || this == KEY_LEFT_BRACKET || this == KEY_BACKSLASH ||
                this == KEY_RIGHT_BRACKET || this == KEY_GRAVE_ACCENT;
    }

    /**
     * Converte para caractere (se for imprimível), senão retorna '\0'.
     */
    public char toChar() {
        if (this == KEY_SPACE) return ' ';
        if (this.value >= 48 && this.value <= 57) return (char) this.value; // 0-9
        if (this.value >= 65 && this.value <= 90) return (char) this.value; // A-Z (maiúsculas)
        // Mapeia teclas de símbolos
        return switch (this) {
            case KEY_APOSTROPHE -> '\'';
            case KEY_COMMA -> ',';
            case KEY_MINUS -> '-';
            case KEY_PERIOD -> '.';
            case KEY_SLASH -> '/';
            case KEY_SEMICOLON -> ';';
            case KEY_EQUAL -> '=';
            case KEY_LEFT_BRACKET -> '[';
            case KEY_BACKSLASH -> '\\';
            case KEY_RIGHT_BRACKET -> ']';
            case KEY_GRAVE_ACCENT -> '`';
            default -> '\0';
        };
    }

    @Override
    public String toString() {
        return this.name() + "(" + this.value + ")";
    }
}
