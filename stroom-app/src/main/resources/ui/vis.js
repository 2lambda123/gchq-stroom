/*
 * Copyright 2016 Crown Copyright
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

var stroomParent;
var stroomFrameId;
var stroomOrigin;

// Send a message to Stroom to open a link
// e.g. `stroomLink('type=Dashboard&uuid=<TARGET_DASHBOARD_UUID>&title=title&params=userId%3Duser2', 'DASHBOARD')`
var stroomLink = function(href, target) {
    target = (typeof target === 'undefined') ? 'browser' : target;

    if (stroomParent && stroomFrameId && stroomOrigin) {
        var obj = {
          frameId : stroomFrameId,
          functionName : 'link',
          href : String(href),
          target : String(target),
        };

        var message = JSON.stringify(obj);
        stroomParent.postMessage(message, stroomOrigin);
    }
}

/**
 * AN OBJECT TO SEND CALLBACK MESSAGES TO Stroom
 */
function Callback(event, frameId, callbackId) {
  stroomParent = event.source;
  stroomFrameId = frameId;
  stroomOrigin = event.origin;

  var event = event;
  var frameId = frameId;
  var callbackId = callbackId;
  var sendCallback = function(event, frameId, callbackId, functionName, param) {
    var obj = {
      frameId : frameId,
      callbackId : callbackId,
      functionName : functionName,
      param : param
    };

    var message = JSON.stringify(obj);

    event.source.postMessage(message, event.origin);
  }

  this.onSuccess = function(message) {
    sendCallback(event, frameId, callbackId, "onSuccess", message);
  };
  this.onFailure = function(ex) {
    sendCallback(event, frameId, callbackId, "onFailure", ex);
  };
}

/**
 * AN OBJECT TO PERFORM SCRIPT INJECTION
 */
function ScriptInjector() {
  var makeScriptElement = function(doc) {
    var script = doc.createElement("script");
    script.setAttribute("type", "text/javascript");
    script.setAttribute("charset", "UTF-8");
    return script;
  }

  var attachListeners = function(scriptElement, url, callback, removeTag) {
    var clearCallbacks = function() {
      scriptElement.onerror = scriptElement.onreadystatechange = scriptElement.onload = null;
      if (removeTag) {
        scriptElement.parentNode.removeChild(scriptElement);
      }
    };
    scriptElement.onload = function() {
      clearCallbacks();
      if (callback) {
        callback.onSuccess(url);
      }
    };
    // or possibly more portable script_tag.addEventListener('error',
    // function(){...}, true);
    scriptElement.onerror = function() {
      clearCallbacks();
      if (callback) {
        var ex = "onerror() called.";
        callback.onFailure(ex);
      }
    };
    scriptElement.onreadystatechange = function() {
      if (/loaded|complete/.test(scriptElement.readyState)) {
        scriptElement.onload();
      }
    };
  }

  this.inject = function(url, callback) {
    var wnd = window;
    var doc = wnd.document;

    var scriptElement = makeScriptElement(doc);
    (doc.head || doc.getElementsByTagName("head")[0]).appendChild(scriptElement);
    attachListeners(scriptElement, url, callback, false);
    scriptElement.src = url;
  }
}

/**
 * AN OBJECT TO LOAD AND INTERACT WITH VISUALISATIONS
 */
function VisualisationManager() {
  var vis = null;
  var currentContext = null;
  var currentSettings = null;
  var currentData = null;
  var running = false;

  var update = function(callback) {
    if (vis && currentData) {
      vis.setData(currentContext, currentSettings, currentData);
    }
  };

  var injectNextScript = function(scripts, callback) {
    if (scripts.length > 0) {
      var script = scripts.splice(0, 1)[0];
      var cb = {
        onSuccess : function(message) {
          injectNextScript(scripts, callback);
        },
        onFailure : function(ex) {
          callback.onFailure("Failed to inject script '" + script.name + "' - "
              + ex.message);
        }
      };

      new ScriptInjector().inject(script.url, cb);

    } else {
      callback.onSuccess(null);
    }
  };

  var doStart = function(callback) {
    running = true;
    if (vis && vis.start) {
      vis.start();
    }
  };

  var doEnd = function(callback) {
    if (vis && vis.end) {
      vis.end();
    }
    running = false;
  };

  this.injectScripts = function(scripts, callback) {
    injectNextScript(scripts, callback);
  };

  this.start = function(callback) {
    doStart();
  };

  this.end = function(callback) {
    doEnd();
  };

  this.setVisType = function(type, callback) {
    try {
      vis = eval("new " + type + "()");
      callback.onSuccess(null);
    } catch (ex) {
      callback.onFailure(ex.message);
    }

    if (vis && vis.element) {
      document.body.innerHTML = "";
      document.body.appendChild(vis.element);
      vis.element.style.width = "100%";
      vis.element.style.height = "100%";
      vis.element.className = "vis";

      if (running) {
        doStart();
        update();
      }
    }
  };

  this.setData = function(context, settings, data, callback) {
    currentContext = context;
    currentSettings = settings;
    currentData = data;

    update();
  };

  this.resize = function(callback) {
    if (vis) {
      vis.resize();
    }
  };
}

/**
 * GLOBAL VISUALISATION MANAGER
 */
var visualisationManager = new VisualisationManager();

/**
 * LISTEN TO WINDOW MESSAGES
 */
var messageListener = function(event) {
  var sendMessage = function(event, frameId, data) {
    var obj = {
      frameId : frameId,
      data : data
    }

    var message = JSON.stringify(obj);

    event.source.postMessage(message, event.origin);
  };

  var origin = event.origin;
  var host = window.location.host;

  // Stop this script being called from other domains.
  if (origin.indexOf(host) === -1)
    return;

  var json = JSON.parse(event.data);

  if (json.data) {
    var frameId;
    var callbackId;

    if (json.frameId) {
      frameId = json.frameId;
    }
    if (json.callbackId) {
      callbackId = json.callbackId;
    }

    var callback = new Callback(event, frameId, callbackId);
    var params = json.data.params;
    if (!params) {
        params = [];
    }
    params.push(callback);

    eval(json.data.functionName + ".apply(this, params);");
  }
}

if (window.addEventListener) {
  addEventListener("message", messageListener, false);
} else {
  attachEvent("onmessage", messageListener);
}
