/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * Olivier Lamy
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.maven.reporters;

import java.util.Collection;
import java.util.Collections;

import hudson.Extension;
import hudson.model.Action;
import hudson.plugins.maven.MavenModule;
import hudson.plugins.maven.MavenModuleSet;
import hudson.plugins.maven.MavenReporterDescriptor;
import hudson.plugins.maven.MojoInfo;

/**
 * Records the javadoc and archives it.
 * 
 * @author Kohsuke Kawaguchi
 */
public class MavenJavadocArchiver extends AbstractMavenJavadocArchiver {


    public Collection<? extends Action> getProjectActions(MavenModule project) {
        return Collections.singletonList(new MavenJavadocAction(project,getTarget(),"Javadoc","javadoc"
                ,hudson.tasks.Messages.JavadocArchiver_DisplayName_Javadoc()));
    }

    @Override
    public String getArchiveTargetPath() {
        return "javadoc";
    }

    public Action getAggregatedProjectAction(MavenModuleSet project) {
        return new MavenJavadocAction(project,getTarget(),"Javadoc","javadoc"
                ,hudson.tasks.Messages.JavadocArchiver_DisplayName_Javadoc());
    }

    @Override
    public boolean checkIsJavadocMojo(MojoInfo mojo) {
        return mojo.is("org.apache.maven.plugins","maven-javadoc-plugin","javadoc")
            || mojo.is("org.apache.maven.plugins","maven-javadoc-plugin","aggregate");
    }

    @Extension
    public static final class DescriptorImpl extends MavenReporterDescriptor {
        public String getDisplayName() {
            return Messages.MavenJavadocArchiver_DisplayName();
        }

        public MavenJavadocArchiver newAutoInstance(MavenModule module) {
            return new MavenJavadocArchiver();
        }
    }

    private static final long serialVersionUID = 1L;
}
