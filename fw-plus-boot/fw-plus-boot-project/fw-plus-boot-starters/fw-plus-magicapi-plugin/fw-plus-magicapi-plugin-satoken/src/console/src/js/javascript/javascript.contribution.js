/*!-----------------------------------------------------------------------------
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Version: 0.32.1(29a273516805a852aa8edc5e05059f119b13eff0)
 * Released under the MIT license
 * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
 *-----------------------------------------------------------------------------*/

// src/basic-languages/javascript/javascript.contribution.ts


import {language,conf} from "./javascript.js";
export default function (monaco) {
  monaco.languages.register({id: "javascript"})
  monaco.languages.setMonarchTokensProvider("javascript",language)
  monaco.languages.setLanguageConfiguration("javascript",conf)
}
