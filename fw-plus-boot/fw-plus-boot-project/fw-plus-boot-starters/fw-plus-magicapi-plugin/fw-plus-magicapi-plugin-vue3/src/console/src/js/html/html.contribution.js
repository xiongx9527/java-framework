/*!-----------------------------------------------------------------------------
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
 * Released under the MIT license
 * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
 *-----------------------------------------------------------------------------*/

// src/basic-languages/html/html.contribution.ts

import {language,conf} from "./html.js";
export default function (monaco) {
  monaco.languages.register({id: "vue3"})
  monaco.languages.setMonarchTokensProvider("vue3",language)
  monaco.languages.setLanguageConfiguration("vue3",conf)

}
