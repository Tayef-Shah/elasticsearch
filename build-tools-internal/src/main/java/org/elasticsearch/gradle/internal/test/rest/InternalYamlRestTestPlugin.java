/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.internal.test.rest;

import org.elasticsearch.gradle.internal.ElasticsearchJavaBasePlugin;
import org.elasticsearch.gradle.internal.ElasticsearchJavaPlugin;
import org.elasticsearch.gradle.internal.test.RestIntegTestTask;
import org.elasticsearch.gradle.internal.test.RestTestBasePlugin;
import org.elasticsearch.gradle.testclusters.TestClustersPlugin;
import org.elasticsearch.gradle.util.GradleUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import static org.elasticsearch.gradle.internal.test.rest.RestTestUtil.createTestCluster;
import static org.elasticsearch.gradle.internal.test.rest.RestTestUtil.registerTask;
import static org.elasticsearch.gradle.internal.test.rest.RestTestUtil.setupDependencies;

/**
 * Apply this plugin to run the YAML based REST tests.
 */
public class InternalYamlRestTestPlugin implements Plugin<Project> {

    public static final String SOURCE_SET_NAME = "yamlRestTest";

    @Override
    public void apply(Project project) {

        project.getPluginManager().apply(ElasticsearchJavaBasePlugin.class);
        project.getPluginManager().apply(TestClustersPlugin.class);
        project.getPluginManager().apply(RestTestBasePlugin.class);
        project.getPluginManager().apply(RestResourcesPlugin.class);

        ElasticsearchJavaPlugin.configureConfigurations(project);
        // create source set
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet yamlTestSourceSet = sourceSets.create(SOURCE_SET_NAME);

        // create the test cluster container
        createTestCluster(project, yamlTestSourceSet);

        // setup the yamlRestTest task
        Provider<RestIntegTestTask> yamlRestTestTask = registerTask(project, yamlTestSourceSet);

        // setup the dependencies
        setupDependencies(project, yamlTestSourceSet);

        // setup the copy for the rest resources
        project.getTasks().withType(CopyRestApiTask.class).configureEach(copyRestApiTask -> {
            copyRestApiTask.setSourceResourceDir(
                yamlTestSourceSet.getResources()
                    .getSrcDirs()
                    .stream()
                    .filter(f -> f.isDirectory() && f.getName().equals("resources"))
                    .findFirst()
                    .orElse(null)
            );
        });

        // Register rest resources with source set
        yamlTestSourceSet.getOutput()
            .dir(
                project.getTasks()
                    .withType(CopyRestApiTask.class)
                    .named(RestResourcesPlugin.COPY_REST_API_SPECS_TASK)
                    .map(CopyRestApiTask::getOutputResourceDir)
            );

        yamlTestSourceSet.getOutput()
            .dir(
                project.getTasks()
                    .withType(CopyRestTestsTask.class)
                    .named(RestResourcesPlugin.COPY_YAML_TESTS_TASK)
                    .map(CopyRestTestsTask::getOutputResourceDir)
            );

        // setup IDE
        GradleUtils.setupIdeForTestSourceSet(project, yamlTestSourceSet);

        // wire this task into check
        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(check -> check.dependsOn(yamlRestTestTask));
    }
}