/*
 * Configurations related with Omega framework.
 *
 * @class
 */
export default class OmegaConfig {
    static petitExecutable = null;

    static setPetitExecutable(petitExecutable) {
        OmegaConfig.petitExecutable = petitExecutable;
    }
}
