package org.sonatype.nexus.restlight.stage;

/**
 * Constant library that contains all of the vocabulary elements that can vary for the staging client.
 */
public final class VocabularyKeys
{

    /**
     * This is the root element of the promote, drop, and finish staging actions. In Nexus Professional 1.3.1, it was
     * <code>com.sonatype.nexus.staging.api.dto.StagingPromoteRequestDTO</code>, but starting in Nexus Professional
     * 1.3.2, the element has been simplified to <code>promoteRequest</code>.
     */
    public static final String PROMOTE_STAGE_REPO_ROOT_ELEMENT = "promoteRepository.rootElement";

    private VocabularyKeys()
    {
    }

}
