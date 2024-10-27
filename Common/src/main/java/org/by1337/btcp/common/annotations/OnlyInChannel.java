package org.by1337.btcp.common.annotations;

import java.lang.annotation.*;

/**
 * Аннотация означает что пакет может быть отправлен только в ChanneledPacket
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface OnlyInChannel {
}
