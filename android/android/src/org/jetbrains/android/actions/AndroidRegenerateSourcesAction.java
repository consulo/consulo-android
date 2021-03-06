/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.actions;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.compiler.AndroidAutogenerator;
import org.jetbrains.android.compiler.AndroidAutogeneratorMode;
import org.jetbrains.android.compiler.AndroidCompileUtil;
import org.jetbrains.android.util.AndroidBundle;
import org.must.android.module.extension.AndroidModuleExtension;
import org.mustbe.consulo.module.extension.ModuleExtensionHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidRegenerateSourcesAction extends AnAction {

  private static final String TITLE = "Generate Sources";

  public AndroidRegenerateSourcesAction() {
    super(TITLE);
  }

  @Override
  public void update(AnActionEvent e) {
    final Module module = e.getData(DataKeys.MODULE);
    final Project project = e.getData(DataKeys.PROJECT);
    boolean visible = project != null && ModuleExtensionHelper.getInstance(project).getModuleExtensions(AndroidModuleExtension.class).size() > 0;
    String title = TITLE;

    if (visible) {
      visible = false;

      if (module != null) {
        AndroidModuleExtension facet = ModuleUtilCore.getExtension(module, AndroidModuleExtension.class);
        if (facet != null) {
          visible = AndroidAutogenerator.supportsAutogeneration(facet);
          title = TITLE + " for '" + module.getName() + "'";
        }
      }
      else {
        Collection<AndroidModuleExtension> facets = ModuleExtensionHelper.getInstance(project).getModuleExtensions(AndroidModuleExtension.class);
        for (AndroidModuleExtension facet : facets) {
          if (AndroidAutogenerator.supportsAutogeneration(facet)) {
            visible = true;
            break;
          }
        }
      }
    }
    e.getPresentation().setVisible(visible);
    e.getPresentation().setEnabled(visible);
    e.getPresentation().setText(title);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(DataKeys.PROJECT);
    Module module = e.getData(DataKeys.MODULE);
    if (module != null) {
      generate(project, module);
      return;
    }
    assert project != null;
    Collection<AndroidModuleExtension> facets = ModuleExtensionHelper.getInstance(project).getModuleExtensions(AndroidModuleExtension.class);
    List<Module> modulesToProcess = new ArrayList<Module>();
    for (AndroidModuleExtension facet : facets) {
      module = facet.getModule();
      if (AndroidAutogenerator.supportsAutogeneration(facet)) {
        modulesToProcess.add(module);
      }
    }
    if (modulesToProcess.size() > 0) {
      generate(project, modulesToProcess.toArray(new Module[modulesToProcess.size()]));
    }
  }

  private static void generate(Project project, final Module... modules) {
    CompilerManager.getInstance(project).executeTask(new CompileTask() {
      @Override
      public boolean execute(CompileContext context) {
        // todo: compatibility with background autogenerating

        for (Module module : modules) {
          final AndroidModuleExtension facet = ModuleUtilCore.getExtension(module, AndroidModuleExtension.class);

          if (facet != null) {
            for (AndroidAutogeneratorMode mode : AndroidAutogeneratorMode.values()) {
              AndroidCompileUtil.generate(facet, mode, context, true);
            }
          }
        }
        return true;
      }
    }, new ModuleCompileScope(project, modules, false), AndroidBundle.message("android.compile.messages.generating.r.java.content.name"),
                                                     null);
  }
}
