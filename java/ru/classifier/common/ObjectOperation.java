package ru.classifier.common;

import java.io.Serializable;

/**
 * User: root
 * Date: 06.04.2008
 * Time: 19:34:59
 */

public interface ObjectOperation extends Serializable {

  public void process(final Object o) ; // todo make exception handling

}
