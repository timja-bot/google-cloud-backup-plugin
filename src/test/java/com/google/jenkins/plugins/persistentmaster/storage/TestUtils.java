/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.persistentmaster.storage;

import javax.annotation.Nullable;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

/**
 * Utility methods for tests.
 *
 * @author akshayd@google.com (Akshay Dayal).
 */
public class TestUtils {

  /**
   * Find an {@link HtmlOption} element in a {@link HtmlForm} with the given
   * text.
   *
   * If no option element is found returns null.
   */
  @Nullable
  public static HtmlOption findOptionWithText(HtmlForm form, String text) {
    for (HtmlSelect select : form.getSelectsByName("")) {
      for (HtmlOption option : select.getOptions()) {
        if (text.equals(option.getText())) {
          return option;
        }
      }
    }

    return null;
  }
}
