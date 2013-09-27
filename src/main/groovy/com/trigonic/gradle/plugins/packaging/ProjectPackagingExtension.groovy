package com.trigonic.gradle.plugins.packaging

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopyProcessingSpec
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.specs.Spec
import org.gradle.internal.reflect.Instantiator

import java.util.regex.Pattern

/**
 * An extension which can be attached to the project. This is a superset of SystemPackagingExtension because we don't
 * want the @Delegate to inherit the copy spec parts.
 *
 * We can't extends DefaultCopySpec, since it's @NotExtensible, meaning that we won't get any convention
 * mappings. If we extend DelegatingCopySpec we get groovy compilation errors around the return types between
 * CopySourceSpec's methods and the ones overriden in DelegatingCopySpec, even though that's perfectly valid
 * Java code. The theory is that it's some bug in groovyc.
 */
public class ProjectPackagingExtension extends SystemPackagingExtension {

    CopySpecInternal delegateCopySpec;

    // @Inject // Not supported yet.
    public ProjectPackagingExtension(Project project) {
        FileResolver resolver = ((ProjectInternal) project).getFileResolver();
        Instantiator instantiator = ((ProjectInternal) project).getServices().get(Instantiator.class);
        delegateCopySpec = new DefaultCopySpec( resolver, instantiator);

    }


    /*
     * Special Use cases that involve Closure's which we want to wrap:
     */
    CopySpec from(Object sourcePath, Closure c) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().from(sourcePath, c);
        }
    }

    CopySpec into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().into(destPath, configureClosure)
        }
    }

    CopySpec include(Closure includeSpec) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().include(includeSpec)
        }
    }

    CopySpec exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().exclude(excludeSpec)
        }
    }

    CopySpec filter(Closure closure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().filter(closure)
        }
    }

    CopySpec rename(Closure closure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().rename(closure)
        }
    }

    CopySpec eachFile(Closure closure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().eachFile(closure)
        }
    }

    /*
     * Copy and Paste from org.gradle.api.internal.file.copy.DelegatingCopySpec, since extending it causes
     * compilation problems. The methods above are special cases and are commented out below.
     */
    public RelativePath getDestPath() {
        return getDelegateCopySpec().getDestPath();
    }

    public FileTree getSource() {
        return getDelegateCopySpec().getSource();
    }

    public boolean hasSource() {
        return getDelegateCopySpec().hasSource();
    }

    public Collection<? extends Action<? super FileCopyDetails>> getAllCopyActions() {
        return getDelegateCopySpec().getAllCopyActions();
    }

    public boolean isCaseSensitive() {
        return getDelegateCopySpec().isCaseSensitive();
    }

    public void setCaseSensitive(boolean caseSensitive) {
        getDelegateCopySpec().setCaseSensitive(caseSensitive);
    }

    public boolean getIncludeEmptyDirs() {
        return getDelegateCopySpec().getIncludeEmptyDirs();
    }

    public void setIncludeEmptyDirs(boolean includeEmptyDirs) {
        getDelegateCopySpec().setIncludeEmptyDirs(includeEmptyDirs);
    }

    public DuplicatesStrategy getDuplicatesStrategy() {
        return getDelegateCopySpec().getDuplicatesStrategy();
    }

    public void setDuplicatesStrategy(DuplicatesStrategy strategy) {
        getDelegateCopySpec().setDuplicatesStrategy(strategy);
    }

    public CopySpec filesMatching(String pattern, Action<? super FileCopyDetails> action) {
        return getDelegateCopySpec().filesMatching(pattern, action);
    }

    public CopySpec filesNotMatching(String pattern, Action<? super FileCopyDetails> action) {
        return getDelegateCopySpec().filesNotMatching(pattern, action);
    }

    public CopySpec with(CopySpec... sourceSpecs) {
        return getDelegateCopySpec().with(sourceSpecs);
    }

    public CopySourceSpec from(Object... sourcePaths) {
        return getDelegateCopySpec().from(sourcePaths);
    }

//    public CopySpec from(Object sourcePath, Closure c) {
//        return getDelegateCopySpec().from(sourcePath, c);
//    }

    public CopySpec setIncludes(Iterable<String> includes) {
        return getDelegateCopySpec().setIncludes(includes);
    }

    public CopySpec setExcludes(Iterable<String> excludes) {
        return getDelegateCopySpec().setExcludes(excludes);
    }

    public CopySpec include(String... includes) {
        return getDelegateCopySpec().include(includes);
    }

    public CopySpec include(Iterable<String> includes) {
        return getDelegateCopySpec().include(includes);
    }

    public CopySpec include(Spec<FileTreeElement> includeSpec) {
        return getDelegateCopySpec().include(includeSpec);
    }

//    public CopySpec include(Closure includeSpec) {
//        return getDelegateCopySpec().include(includeSpec);
//    }

    public CopySpec exclude(String... excludes) {
        return getDelegateCopySpec().exclude(excludes);
    }

    public CopySpec exclude(Iterable<String> excludes) {
        return getDelegateCopySpec().exclude(excludes);
    }

    public CopySpec exclude(Spec<FileTreeElement> excludeSpec) {
        return getDelegateCopySpec().exclude(excludeSpec);
    }

//    public CopySpec exclude(Closure excludeSpec) {
//        return getDelegateCopySpec().exclude(excludeSpec);
//    }

    public CopySpec into(Object destPath) {
        return getDelegateCopySpec().into(destPath);
    }

//    public CopySpec into(Object destPath, Closure configureClosure) {
//        return getDelegateCopySpec().into(destPath, configureClosure);
//    }

//    public CopySpec rename(Closure closure) {
//        return getDelegateCopySpec().rename(closure);
//    }

    public CopySpec rename(String sourceRegEx, String replaceWith) {
        return getDelegateCopySpec().rename(sourceRegEx, replaceWith);
    }

    public CopyProcessingSpec rename(Pattern sourceRegEx, String replaceWith) {
        return getDelegateCopySpec().rename(sourceRegEx, replaceWith);
    }

    public CopySpec filter(Map<String, ?> properties, Class<? extends FilterReader> filterType) {
        return getDelegateCopySpec().filter(properties, filterType);
    }

    public CopySpec filter(Class<? extends FilterReader> filterType) {
        return getDelegateCopySpec().filter(filterType);
    }

//    public CopySpec filter(Closure closure) {
//        return getDelegateCopySpec().filter(closure);
//    }

    public CopySpec expand(Map<String, ?> properties) {
        return getDelegateCopySpec().expand(properties);
    }

    public CopySpec eachFile(Action<? super FileCopyDetails> action) {
        return getDelegateCopySpec().eachFile(action);
    }

//    public CopySpec eachFile(Closure closure) {
//        return getDelegateCopySpec().eachFile(closure);
//    }

    public Integer getFileMode() {
        return getDelegateCopySpec().getFileMode();
    }

    public CopyProcessingSpec setFileMode(Integer mode) {
        return getDelegateCopySpec().setFileMode(mode);
    }

    public Integer getDirMode() {
        return getDelegateCopySpec().getDirMode();
    }

    public CopyProcessingSpec setDirMode(Integer mode) {
        return getDelegateCopySpec().setDirMode(mode);
    }

    public Set<String> getIncludes() {
        return getDelegateCopySpec().getIncludes();
    }

    public Set<String> getExcludes() {
        return getDelegateCopySpec().getExcludes();
    }

    public Iterable<CopySpecInternal> getChildren() {
        return getDelegateCopySpec().getChildren();
    }

    public FileTree getAllSource() {
        return getDelegateCopySpec().getAllSource();
    }

    public DefaultCopySpec addChild() {
        return getDelegateCopySpec().addChild();
    }

    public DefaultCopySpec addFirst() {
        return getDelegateCopySpec().addFirst();
    }

    public void walk(Action<? super CopySpecInternal> action) {
        action.execute(this);
        for (CopySpecInternal child : getChildren()) {
            child.walk(action);
        }
    }

}
