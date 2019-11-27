package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import io.jenkins.plugins.gitlabbranchsource.retry.GitLabApiRetryWrapper;
import java.io.IOException;
import jenkins.plugins.git.GitTagSCMRevision;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import org.gitlab4j.api.GitLabApiException;

public class GitLabSCMFileSystem extends SCMFileSystem {

    private final GitLabApiRetryWrapper gitLabApi;
    private final String projectPath;
    private final String ref;

    protected GitLabSCMFileSystem(
        GitLabApiRetryWrapper gitLabApi,
        String projectPath,
        String ref,
        @CheckForNull SCMRevision rev) throws IOException {
        super(rev);
        this.gitLabApi = gitLabApi;
        this.projectPath = projectPath;
        if (rev != null) {
            if (rev.getHead() instanceof MergeRequestSCMHead) {
                this.ref = ((MergeRequestSCMRevision) rev).getOrigin().getHash();
            } else if (rev instanceof BranchSCMRevision) {
                this.ref = ((BranchSCMRevision) rev).getHash();
            } else if (rev instanceof GitTagSCMRevision) {
                this.ref = ((GitTagSCMRevision) rev).getHash();
            } else {
                this.ref = ref;
            }
        } else {
            this.ref = ref;
        }
    }

    @Override
    public long lastModified() throws IOException {
        try {
            return gitLabApi.getCommitsApi().getCommit(projectPath, ref).getCommittedDate().getTime();
        } catch (GitLabApiException e) {
            return 0;
        }
    }

    @NonNull
    @Override
    public SCMFile getRoot() {
        return new GitLabSCMFile(gitLabApi, projectPath, ref);
    }

    @Extension
    public static class BuilderImpl extends Builder {

        @Override
        public boolean supports(SCM source) {
            return false;
        }

        @Override
        public boolean supports(SCMSource source) {
            return source instanceof GitLabSCMSource;
        }

        @Override
        protected boolean supportsDescriptor(SCMDescriptor scmDescriptor) {
            return false;
        }

        @Override
        protected boolean supportsDescriptor(SCMSourceDescriptor scmSourceDescriptor) {
            return false;
        }

        @Override
        public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm,
            @CheckForNull SCMRevision rev) {
            return null;
        }

        public SCMFileSystem build(@NonNull SCMHead head, @CheckForNull SCMRevision rev,
                                   @NonNull GitLabApiRetryWrapper gitLabApi, @NonNull String projectPath)
            throws IOException, InterruptedException {
            String ref;
            if (head instanceof MergeRequestSCMHead) {
                ref = ((MergeRequestSCMHead) head).getOriginName();
            } else if (head instanceof BranchSCMHead) {
                ref = head.getName();
            } else if (head instanceof GitLabTagSCMHead) {
                ref = head.getName();
            } else {
                return null;
            }
            return new GitLabSCMFileSystem(gitLabApi, projectPath, ref, rev);
        }
    }
}
