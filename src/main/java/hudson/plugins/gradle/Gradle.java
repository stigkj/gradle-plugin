package hudson.plugins.gradle;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import net.sf.json.JSONObject;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.util.UncheckedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;


/**
 * @author Gregory Boissinot
 */
public class Gradle extends Builder {

    /**
     * The GradleBuilder build step description
     */
    private final String description;

    /**
     * The GradleBuilder command line switches
     */
    private final String switches;

    /**
     * The GradleBuilder tasks
     */
    private final String tasks;


    private final String rootBuildScriptDir;

    /**
     * The GradleBuilder build file path
     */
    private final String buildFile;

    /**
     * Identifies {@link GradleInstallation} to be used.
     */
    private final String gradleName;

    /**
     * Flag whether to use the gradle wrapper rather than a standard Gradle installation
     */
    private final boolean useWrapper;

    @DataBoundConstructor
    public Gradle(String description, String switches, String tasks, String rootBuildScriptDir, String buildFile,
                  String gradleName, boolean useWrapper) {
        this.description = description;
        this.switches = switches;
        this.tasks = tasks;
        this.gradleName = gradleName;
        this.rootBuildScriptDir = rootBuildScriptDir;
        this.buildFile = buildFile;
        this.useWrapper = !useWrapper;
    }


    public String getSwitches() {
        return switches;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public String getGradleName() {
        return gradleName;
    }

    public String getTasks() {
        return tasks;
    }

    public String getDescription() {
        return description;
    }

    public boolean isUseWrapper() {
        return useWrapper;
    }

    public String getRootBuildScriptDir() {
        return rootBuildScriptDir;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher l, final BuildListener listener)
            throws InterruptedException, IOException {
        GradleConnector connector = GradleConnector.newConnector();
        ProjectConnection connection =
                connector.
                    forProjectDirectory(projectDir(build)).
                    useInstallation(gradleInstallationDir(build, listener)).
                        connect();

        final EclipseProject model = connection.getModel(EclipseProject.class);
        for (EclipseProject child : model.getChildren()){
            System.out.println("child.getSourceDirectories() = " + new ArrayList(child.getSourceDirectories().getAll()));
            System.out.println("child.getProjectDependencies() = " + new ArrayList(child.getProjectDependencies().getAll()));
        }

        BuildLauncher launcher = connection.newBuild();
        final PrintStream logger = listener.getLogger();
        final BlockingResultHandler<Void> handler = new BlockingResultHandler<Void>(Void.class);

        launcher.forTasks(tasks.split("[ ,]")).
                setStandardOutput(logger).setStandardError(logger).run(handler);

        handler.getResult();
        connection.close();

        return true;
    }

    private File gradleInstallationDir(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        GradleInstallation gradleInstallation = null;

        for (GradleInstallation gi : getDescriptor().getInstallations()) {
            if (gradleName != null && gi.getName().equals(gradleName)) {
                gradleInstallation = gi;
            }
        }

        if (gradleInstallation == null) {
            throw new IllegalStateException("No Gradle installation selected");
        }

        gradleInstallation = gradleInstallation.forEnvironment(build.getEnvironment(listener));
        gradleInstallation = gradleInstallation.forNode(Computer.currentComputer().getNode(), listener);

        return new File(gradleInstallation.getHome());
    }

    private File projectDir(final AbstractBuild<?, ?> build) {
        FilePath workspace = build.getModuleRoot();
        return new File(workspace.getRemote());
    }

    public static class BlockingResultHandler<T> implements ResultHandler<T> {
        private final BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1);
        private final Class<T> resultType;
        private static final Object NULL = new Object();

        public BlockingResultHandler(Class<T> resultType) {
            this.resultType = resultType;
        }

        public T getResult() {
            Object result;
            try {
                result = queue.take();
            } catch (InterruptedException e) {
                throw UncheckedException.asUncheckedException(e);
            }

            if (result instanceof Throwable) {
                throw UncheckedException.asUncheckedException((Throwable) result);
            }
            if (result == NULL) {
                return null;
            }
            return resultType.cast(result);
        }

        public void onComplete(T result) {
            queue.add(result == null ? NULL : result);
        }

        public void onFailure(GradleConnectionException failure) {
            queue.add(failure);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @CopyOnWrite
        private volatile GradleInstallation[] installations = new GradleInstallation[0];

        public DescriptorImpl() {
            load();
        }

        protected DescriptorImpl(Class<? extends Gradle> clazz) {
            super(clazz);
        }

        /**
         * Obtains the {@link GradleInstallation.DescriptorImpl} instance.
         */
        public GradleInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(GradleInstallation.DescriptorImpl.class);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        protected void convert(Map<String, Object> oldPropertyBag) {
            if (oldPropertyBag.containsKey("installations")) {
                installations = (GradleInstallation[]) oldPropertyBag.get("installations");
            }
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gradle/help.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.step_displayName();
        }

        public GradleInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(GradleInstallation... installations) {
            this.installations = installations;
            save();
        }

        @Override
        public Gradle newInstance(StaplerRequest request, JSONObject formData) throws FormException {
            return (Gradle) request.bindJSON(clazz, formData);
        }
    }
}
