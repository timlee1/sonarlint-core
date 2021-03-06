/*
 * SonarLint Core - Implementation
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.core.analyzer.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonarsource.api.sonarlint.SonarLintSide;

@SonarLintSide
public class SensorOptimizer {

  private static final Logger LOG = LoggerFactory.getLogger(SensorOptimizer.class);

  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final Settings settings;

  public SensorOptimizer(FileSystem fs, ActiveRules activeRules, Settings settings) {
    this.fs = fs;
    this.activeRules = activeRules;
    this.settings = settings;
  }

  /**
   * Decide if the given Sensor should be executed.
   */
  public boolean shouldExecute(DefaultSensorDescriptor descriptor) {
    if (!fsCondition(descriptor)) {
      LOG.debug("'{}' skipped because there is no related file in current project", descriptor.name());
      return false;
    }
    if (!activeRulesCondition(descriptor)) {
      LOG.debug("'{}' skipped because there is no related rule activated in the quality profile", descriptor.name());
      return false;
    }
    if (!settingsCondition(descriptor)) {
      LOG.debug("'{}' skipped because one of the required properties is missing", descriptor.name());
      return false;
    }
    return true;
  }

  private boolean settingsCondition(DefaultSensorDescriptor descriptor) {
    if (!descriptor.properties().isEmpty()) {
      for (String propertyKey : descriptor.properties()) {
        if (!settings.hasKey(propertyKey)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean activeRulesCondition(DefaultSensorDescriptor descriptor) {
    if (!descriptor.ruleRepositories().isEmpty()) {
      for (String repoKey : descriptor.ruleRepositories()) {
        if (!activeRules.findByRepository(repoKey).isEmpty()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean fsCondition(DefaultSensorDescriptor descriptor) {
    if (!descriptor.languages().isEmpty() || descriptor.type() != null) {
      FilePredicate langPredicate = descriptor.languages().isEmpty() ? fs.predicates().all() : fs.predicates().hasLanguages(descriptor.languages());

      FilePredicate typePredicate = descriptor.type() == null ? fs.predicates().all() : fs.predicates().hasType(descriptor.type());
      return fs.hasFiles(fs.predicates().and(langPredicate, typePredicate));
    }
    return true;
  }

}
