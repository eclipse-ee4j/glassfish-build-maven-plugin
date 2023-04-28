/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.build.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.apache.tools.ant.types.ZipFileSet;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Helper for common Maven Plugin tasks.
 */
public final class MavenHelper {

    /**
     * Cannot be instantiated.
     */
    private MavenHelper() {
    }

    /**
     * Reads a given model.
     * @param pom the pom File
     * @return an instance of Model
     * @throws MojoExecutionException if an {@code IOException} occurs
     */
    public static Model readModel(final File pom)
            throws MojoExecutionException {

        try {
           return new DefaultModelReader().read(pom, null);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Get the final name of a project artifact from a {@code Model} instance.
     * @param model the model
     * @return the final name of the artifact
     */
    private static String getFinalName(final Model model) {
        Build build = model.getBuild();
        String finalName;
        if (build != null && build.getFinalName() != null) {
            finalName = build.getFinalName();
        } else {
            String version;
            if (model.getVersion() != null) {
                version = model.getVersion();
            } else {
                if (model.getParent() == null) {
                    throw new IllegalStateException(
                            "no version defined and no parent found");
                }
                version = model.getParent().getVersion();
            }
            finalName = model.getArtifactId() + "-" + version;
        }
        return finalName;
    }

    /**
     * Create a list of attached artifacts and their associated files by
     * searching for <code>target/${project.build.finalName}-*.*</code>.
     * @param dir ${project.build.directory}
     * @param artifact the main artifact for which corresponding attached
     * artifacts will be searched
     * @param model an instance of model
     * @return the list of attached artifacts
     * @throws MojoExecutionException if an error occurred while searching
     * files
     */
    public static List<Artifact> createAttachedArtifacts(final String dir,
            final Artifact artifact,
            final Model model) throws MojoExecutionException {

        if (dir == null || dir.isEmpty()) {
            throw new IllegalArgumentException("dir is null or empty");
        }
        if (artifact == null) {
            throw new IllegalArgumentException("artifact is null");
        }
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }

        // compute finalName
        String artifactName = "";
        String finalName;
        if (artifact.getFile() != null && artifact.getFile().exists()) {
            artifactName = artifact.getFile().getName();
            finalName = artifactName.substring(0,
                    artifactName.lastIndexOf('.'));
        } else {
            finalName = getFinalName(model);
        }

        List<File> attachedFiles = getFiles(dir, finalName + "*.*",
                artifactName);
        List<Artifact> attachedArtifacts = new ArrayList<>();
        if (!attachedFiles.isEmpty()) {
            for (File attached : attachedFiles) {
                String tokens = attached.getName().substring(
                        finalName.length());

                // pom is not an attached artifact
                if (tokens.endsWith(".pom")) {
                    continue;
                }

                String type;
                if (tokens.endsWith(".asc")) {
                    // compute type as xxx.asc
                    type = tokens.substring(
                            tokens.substring(0,
                                    tokens.length()
                                    - ".asc".length()).lastIndexOf('.')
                                    + 1,
                            tokens.length());
                } else {
                    type = tokens.substring(
                            tokens.lastIndexOf('.') + 1,
                            tokens.length());
                }

                String classifier;
                if (tokens.endsWith(".pom.asc")) {
                    // pom.asc does not have any classifier
                    classifier = "";
                } else {
                    // classifier = tokens - type
                    classifier = tokens.substring(
                            tokens.lastIndexOf('-') + 1,
                            tokens.length() - (type.length() + 1));

                    if (classifier.contains(artifact.getVersion())) {
                        classifier = classifier.substring(
                                classifier.indexOf(artifact.getVersion() + 1,
                                classifier.length()
                                        - (artifact.getVersion().length())));
                    }
                }

                Artifact attachedArtifact = createArtifact(model, type,
                        classifier);
                attachedArtifact.setFile(attached);
                attachedArtifacts.add(attachedArtifact);
            }
        }
        return attachedArtifacts;
    }

    /**
     * Search for a file matching an artifact for the given {@code finalName}
     * and model.
     * @param dir the directory to search
     * @param finalName the artifact final name
     * @param model the project model
     * @return the {@code Artifact} instance
     * @throws MojoExecutionException if an error occurred while searching
     * files
     */
    private static Artifact getArtifactFile(final String dir,
            final String finalName,
            final Model model) throws MojoExecutionException {

        if (dir == null || dir.isEmpty()) {
            throw new IllegalArgumentException("dir is null or empty");
        }
        if (finalName == null || finalName.isEmpty()) {
            throw new IllegalArgumentException("finalName is null");
        }
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }

        List<File> files = getFiles(dir, finalName + ".*", finalName + "-*.");
        Map<String, File> extensionMap =
                new HashMap<>(files.size());
        for (File file : files) {
            extensionMap.put(file.getName().substring(finalName.length() + 1),
                    file);
        }

        // 1. guess the extension from the packaging
        File artifactFile = extensionMap.get(model.getPackaging());
        if (artifactFile != null) {
            Artifact artifact = createArtifact(model);
            artifact.setFile(artifactFile);
            return artifact;
        }

        // 2. take what's available
        for (String ext : extensionMap.keySet()) {
            if (!ext.equals("pom") && !ext.endsWith(".asc")) {
                // packaging does not match the type
                // hence we provide type = ext
                Artifact artifact = createArtifact(model, ext,
                        /* classifier */ null);
                artifact.setFile(extensionMap.get(ext));
                return artifact;
            }
        }
        return null;
    }

    /**
     * Create an artifact and its associated file by searching for
     * <code>target/${project.build.finalName}.${project.packaging}</code>.
     * @param dir ${project.build.directory}
     * @param model an instance of model
     * @return the created {@code Artifact} instance
     * @throws MojoExecutionException if an error occurred while searching
     * files
     */
    public static Artifact createArtifact(final String dir, final Model model)
            throws MojoExecutionException {

        // resolving using finalName
        Artifact artifact = getArtifactFile(dir, getFinalName(model), model);
        if (artifact == null) {
            // resolving using artifactId
            artifact = getArtifactFile(dir, model.getArtifactId(), model);
        }
        return artifact;
    }

    /**
     * Returns the pom installed in target or null if not found.
     * @param dir ${project.build.directory}
     * @return an instance of the pom file or null if not found
     * @throws MojoExecutionException if an error occurred while searching
     * files
     */
    public static File getPomInTarget(final String dir)
            throws MojoExecutionException {

        // check for an existing .pom
         List<File> poms = getFiles(dir, /* includes */ "*.pom",
                 /* excludes */ "");
         if (!poms.isEmpty()) {
            return poms.get(0);
         }
         return null;
    }

    /**
     * Return the files contained in the directory, using inclusion and
     * exclusion ant patterns.
     * @param dirPath the directory to scan
     * @param includes the includes pattern, comma separated
     * @param excludes the excludes pattern, comma separated
     * @return the list of files found
     * @throws MojoExecutionException if an IOException occurred
     */
    public static List<File> getFiles(final String dirPath,
            final String includes,
            final String excludes) throws MojoExecutionException {

        if (dirPath == null || dirPath.isEmpty()) {
            throw new IllegalArgumentException("dir is null or empty");
        }

        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            try {
                return FileUtils.getFiles(dir, includes, excludes);
            } catch (IOException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Creates an artifact instance for the supplied coordinates.
     * @param groupId The groupId
     * @param artifactId The artifactId
     * @param version The version
     * @param type The type of the artifact. e.g "jar", "war" or "zip"
     * @param classifier The classifier
     * @return the created artifact
     */
    public static Artifact createArtifact(final String groupId,
            final String artifactId,
            final String version,
            final String type,
            final String classifier) {

        return new DefaultArtifact(groupId, artifactId,
                VersionRange.createFromVersion(version),
                /* scope */ "runtime", type, classifier,
                new DefaultArtifactHandler(type));
    }

    /**
     * Creates an artifact instance for the supplied model.
     * @param model the model
     * @param type the type of the artifact
     * @param classifier the classifier to use
     * @return the created artifact
     */
    public static Artifact createArtifact(final Model model,
            final String type,
            final String classifier) {

        String groupId = model.getGroupId();
        if (groupId == null) {
            if (model.getParent() == null) {
                throw new IllegalStateException(
                        "groupId is null and parent is null");
            }
            groupId = model.getParent().getGroupId();
        }

        String version = model.getVersion();
        if (version == null) {
            if (model.getParent() == null) {
                throw new IllegalStateException(
                        "version is null and parent is null");
            }
            version = model.getParent().getVersion();
        }

        return createArtifact(groupId, model.getArtifactId(), version, type,
                classifier);
    }

    /**
     * Creates an artifact instance for the supplied model.
     *
     * @param model the model
     * @return the created artifact
     */
    public static Artifact createArtifact(final Model model) {
        return createArtifact(model, model.getPackaging(),
                /* classifier */ null);
    }

    /**
     * Creates an artifact instance for the supplied coordinates.
     * @param groupId The groupId
     * @param artifactId The artifactId
     * @param version The version
     * @param type The type of the artifact. e.g "jar", "war" or "zip"
     * @return the created artifact
     */
    public static Artifact createArtifact(final String groupId,
            final String artifactId,
            final String version,
            final String type) {

        return createArtifact(groupId, artifactId, version, type,
                /* classifier */ null);
    }

    /**
     * Creates an artifact instance from a dependency object.
     * @param dep the dependency object
     * @return the created artifact
     */
    public static Artifact createArtifact(final Dependency dep) {
        return createArtifact(dep.getGroupId(), dep.getArtifactId(),
                dep.getVersion(), dep.getType(), dep.getClassifier());
    }

    /**
     * Write a model to <code>buildDir/${project.build.finalName}.pom</code>.
     * @param model an instance of model
     * @param buildDir the directory in which to write the pom
     * @throws IOException if an error occurred while writing the file
     */
    public static void writePom(final Model model, final File buildDir)
            throws IOException {

        writePom(model, buildDir, /* pomFileName */ null);
    }

    /**
     * Write the model to <code>buildDir/${project.build.finalName}.pom</code>.
     * @param model an instance of model
     * @param buildDir the directory in which to write the pom
     * @param pomFileName the name of the written pom
     * @throws IOException if an error occurred while writing the file
     */
    public static void writePom(final Model model,
            final File buildDir,
            final String pomFileName) throws IOException {

        String pom;
        if (pomFileName == null) {
            if (model.getBuild() != null
                    && model.getBuild().getFinalName() != null) {
                pom = model.getBuild().getFinalName() + ".pom";
            } else {
                pom = "pom.xml";
            }
        } else {
            pom = pomFileName;
        }

        File pomFile = new File(buildDir, pom);
        new DefaultModelWriter().write(pomFile, /* options */ null, model);
        model.setPomFile(pomFile);
    }

    /**
     * Read a model as a {@code String}.
     * @param model the model
     * @return the model as a {@code String}
     * @throws MojoExecutionException if an IOException occurred
     */
    public static String modelAsString(final Model model)
            throws MojoExecutionException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new DefaultModelWriter().write(baos, /* options */ null, model);
            return new String(baos.toByteArray());
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Filters a set of artifacts.
     * @param artifacts the set of artifacts to filter
     * @param dependencyArtifacts the set of artifact representing direct
     * dependencies
     * @param excludeTransitive exclude transitive dependencies
     * @param includeScope the scopes to include, comma separated, can be null
     * @param excludeScope the scopes to exclude, comma separated, can be null
     * @param excludeTypes the types to include, comma separated, can be null
     * @param includeTypes the types to exclude, comma separated, can be null
     * @return the set of filtered artifacts
     * @throws MojoExecutionException if an error occurred while filtering
     */
    public static Set<Artifact> filterArtifacts(final Set<Artifact> artifacts,
            final Set<Artifact> dependencyArtifacts,
            final boolean excludeTransitive,
            final String includeScope,
            final String excludeScope,
            final String excludeTypes,
            final String includeTypes) throws MojoExecutionException {

        return filterArtifacts(artifacts, dependencyArtifacts,
                excludeTransitive, includeScope, excludeScope, includeTypes,
                excludeTypes, /* includeClassifiers */ null,
                /* excludeClassifiers */ null, /* includeGroupIds */ null,
                /* excludeGroupIds */ null, /* includeArtifactIds */ null,
                /* excludeArtifactIds */ null);
    }

    /**
     * Filters a set of artifacts.
     * @param artifacts the set of artifacts to filter
     * @param dependencyArtifacts the set of artifact representing direct
     * dependencies
     * @return the set of filtered artifacts
     * @throws MojoExecutionException if an error occurred while filtering
     */
    public static Set<Artifact> excludeTransitive(final Set<Artifact> artifacts,
            final Set<Artifact> dependencyArtifacts)
            throws MojoExecutionException {

        return filterArtifacts(artifacts, dependencyArtifacts,
                /* excludeTransitive */ true, /* includeScope */ null,
                /* excludeScope */ null, /* includeTypes */ null,
                /* excludeTypes */ null, /* includeClassifiers */ null,
                /* excludeClassifiers */ null, /* includeGroupIds */ null,
                /* excludeGroupIds */ null, /* includeArtifactIds */ null,
                /* excludeArtifactIds */ null);
    }

    /**
     * Filters a set of artifacts.
     * @param artifacts the set of artifacts to filter
     * @param dependencyArtifacts the set of artifact representing direct
     * dependencies
     * @param excludeTransitive exclude transitive dependencies
     * @param includeScope the scopes to include, comma separated, can be
     * {@code null}
     * @param excludeScope the scopes to exclude, comma separated, can be
     * {@code null}
     * @param excludeTypes the types to exclude, comma separated, can be
     * {@code null}
     * @param includeTypes the types to include, comma separated, can be
     * {@code null}
     * @param includeClassifiers the classifiers to include, comma separated,
     * can be {@code null}
     * @param excludeClassifiers the classifiers to exclude, comma separated,
     * can be {@code null}
     * @param includeGroupIds the groupIds to include, comma separated,
     * can be {@code null}
     * @param excludeGroupIds the groupIds to exclude, comma separated,
     * can be {@code null}
     * @param includeArtifactIds the artifactIds to include, comma separated,
     * can be {@code null}
     * @param excludeArtifactIds the artifactIds to exclude, comma separated,
     * can be {@code null}
     * @return the set of filtered artifacts
     * @throws MojoExecutionException if an error occurred while filtering
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static Set<Artifact> filterArtifacts(final Set<Artifact> artifacts,
            final Set<Artifact> dependencyArtifacts,
            final boolean excludeTransitive,
            final String includeScope,
            final String excludeScope,
            final String excludeTypes,
            final String includeTypes,
            final String includeClassifiers,
            final String excludeClassifiers,
            final String includeGroupIds,
            final String excludeGroupIds,
            final String includeArtifactIds,
            final String excludeArtifactIds) throws MojoExecutionException {


        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter(new ProjectTransitivityFilter(
                dependencyArtifacts,
                excludeTransitive));

        filter.addFilter(new ScopeFilter(
                cleanToBeTokenizedString(includeScope),
                cleanToBeTokenizedString(excludeScope)));

        filter.addFilter(new TypeFilter(
                cleanToBeTokenizedString(includeTypes),
                cleanToBeTokenizedString(excludeTypes)));

        filter.addFilter(new ClassifierFilter(
                cleanToBeTokenizedString(includeClassifiers),
                cleanToBeTokenizedString(excludeClassifiers)));

        filter.addFilter(new GroupIdFilter(
                cleanToBeTokenizedString(includeGroupIds),
                cleanToBeTokenizedString(excludeGroupIds)));

        filter.addFilter(new ArtifactIdFilter(
                cleanToBeTokenizedString(includeArtifactIds),
                cleanToBeTokenizedString(excludeArtifactIds)));

        try {
            return filter.filter(artifacts);
        } catch (ArtifactFilterException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Unpacks a given file.
     * @param file the file to unpack
     * @param location the directory where to unpack
     * @param includes includes pattern for the files to unpack
     * @param excludes exclude pattern for the files to unpack
     * @param silent log unpack or not
     * @param log the Maven logger instance, can be null
     * @param archiverManager an instance of ArchiveManager
     * @throws MojoExecutionException if an error occurred while unpacking
     */
    public static void unpack(final File file,
            final File location,
            final String includes,
            final String excludes,
            final boolean silent,
            final Log log,
            final ArchiverManager archiverManager)
            throws MojoExecutionException {

        if (log != null && log.isInfoEnabled() && !silent) {
            log.info(logUnpack(file, location, includes, excludes));
        }

        location.mkdirs();

        try {
            UnArchiver unArchiver = archiverManager.getUnArchiver(file);
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);

            if (StringUtils.isNotEmpty(excludes)
                    || StringUtils.isNotEmpty(includes)) {

                IncludeExcludeFileSelector[] selectors =
                        new IncludeExcludeFileSelector[]{
                    new IncludeExcludeFileSelector()
                };

                if (StringUtils.isNotEmpty(excludes)) {
                    selectors[0].setExcludes(excludes.split(","));
                }
                if (StringUtils.isNotEmpty(includes)) {
                    selectors[0].setIncludes(includes.split(","));
                }
                unArchiver.setFileSelectors(selectors);
            }

            unArchiver.extract();
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Unknown archiver type", e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException(
                    "Error unpacking file: " + file + " to: " + location
                            + "\r\n" + e.toString(), e);
        }
    }

    /**
     * Create the logging message for an unpack invocation.
     * @param file the file being unpacked
     * @param location the target directory for the unpack
     * @param includes the include patterns
     * @param excludes the exclude patterns
     * @return the created {@code String}
     */
    private static String logUnpack(final File file,
            final File location,
            final String includes,
            final String excludes) {

        StringBuilder msg = new StringBuilder();
        msg.append("Unpacking ");
        msg.append(file);
        msg.append(" to ");
        msg.append(location);

        if (includes != null && excludes != null) {
            msg.append(" with includes \"");
            msg.append(includes);
            msg.append("\" and excludes \"");
            msg.append(excludes);
            msg.append("\"");
        } else if (includes != null) {
            msg.append(" with includes \"");
            msg.append(includes);
            msg.append("\"");
        } else if (excludes != null) {
            msg.append(" with excludes \"");
            msg.append(excludes);
            msg.append("\"");
        }

        return msg.toString();
    }

    /**
     * Resolve a remote artifact using aether.
     * @param groupId the group identifier of the artifact, may be {@code null}
     * @param artifactId the artifact identifier of the artifact, may be
     * {@code null}
     * @param classifier the classifier of the artifact, may be {@code null}
     * @param type the type of the artifact, may be {@code null}
     * @param version the version of the artifact, may be {@code null}
     * @param repoSystem the repository system component
     * @param repoSession the repository session component
     * @param remoteRepos the remote repositories to use
     * @return the resolved artifact
     * @throws MojoExecutionException if an error occurred while resolving the
     * artifact
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static ArtifactResult resolveArtifact(final String groupId,
            final String artifactId,
            final String classifier,
            final String type,
            final String version,
            final RepositorySystem repoSystem,
            final RepositorySystemSession repoSession,
            final List<RemoteRepository> remoteRepos)
            throws MojoExecutionException {

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new org.eclipse.aether.artifact.DefaultArtifact(
                groupId, artifactId, classifier, type, version));
        request.setRepositories(remoteRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Clean the pattern string for future regexp usage.
     * @param str the string to cleanup
     * @return the cleaned string
     */
    public static String cleanToBeTokenizedString(final String str) {
        String ret = "";
        if (!StringUtils.isEmpty(str)) {
            ret = str.trim().replaceAll("[\\s]*,[\\s]*", ",");
        }
        return ret;
    }

    /**
     * Write the given input to a file.
     * @param outfile the file to write to
     * @param input the input to write
     * @throws IOException if an error occurs while writing to the file
     */
    public static void writeFile(final File outfile, final StringBuilder input)
            throws IOException {

        Writer writer = WriterFactory.newXmlWriter(outfile);
        try {
            IOUtil.copy(input.toString(), writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Convert a comma separated string into a list.
     * @param list the string containing items separated by comma(s)
     * @return list
     */
    public static List<String> getCommaSeparatedList(final String list) {
        if (list != null) {
            String[] listArray = list.split(",");
            if (listArray != null) {
                return Arrays.asList(listArray);
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Convert a to a comma separated {@code String}.
     * @param list the {@code List} to convert
     * @return the resulting {@code String}
     */
    private static String listToString(final List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /**
     * Create an Ant {@code ZipFileSet}.
     * @param dir the resource directory
     * @param includes the list of include patterns
     * @param excludes the list of exclude patterns
     * @return the create {@code ZipFileSet}
     */
    public static ZipFileSet createZipFileSet(final File dir,
            final List<String> includes,
            final List<String> excludes) {

        return createZipFileSet(dir, listToString(includes),
                listToString(excludes));
    }

    /**
     * Create an Ant {@code ZipFileSet}.
     * @param dir the resource directory
     * @param includePatterns the include patterns in comma separated list
     * @param excludePatterns the exclude patterns in comma separate list
     * @return the create {@code ZipFileSet}
     */
    public static ZipFileSet createZipFileSet(final File dir,
            final String includePatterns,
            final String excludePatterns) {

        String includes = includePatterns;
        if (includePatterns == null) {
            includes = "";
        }
        String excludes = excludePatterns;
        if (excludePatterns == null) {
            excludes = "";
        }
        ZipFileSet fset = new ZipFileSet();
        fset.setDir(dir);
        fset.setIncludes(includes);
        fset.setExcludes(excludes);
        fset.setDescription(String.format(
                "file set: %s ( excludes: [ %s ], includes: [ %s ])",
                dir.getAbsolutePath(), excludes, includes));
        return fset;
    }

    /**
     * Create a zip file.
     * @param props Ant project properties
     * @param log Maven logger
     * @param duplicate behavior for duplicate file, one of "add", "preserve"
     * or "fail"
     * @param fsets list of {@code ZipFileSet} that describe the resources to
     * zip
     * @param target the {@code File} instance for the zip file to create
     * @param timestamp optional reproducible build timestamp
     * @return the target file
     */
    public static File createZip(final Properties props,
            final Log log,
            final String duplicate,
            final List<ZipFileSet> fsets,
            final File target,
            final Optional<Instant> timestamp) {

        ZipHelper.getInstance().zip(props, log, duplicate, fsets, target, timestamp);
        return target;
    }
}
