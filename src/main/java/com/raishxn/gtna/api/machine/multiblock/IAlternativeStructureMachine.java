package com.raishxn.gtna.api.machine.multiblock;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;

import java.util.List;

/**
 * A multiblock that also forms from a whole second structure built <b>instead of</b> its main pattern and its
 * modules (the GTNH Nano Forge tier 4 is not an extension of the tower: it replaces it). Modules
 * ({@link ISubPatternMachine}, {@link GTNASubPatterns}) are added on top of the main pattern; an alternative
 * structure is never combined with them, because its blocks claim cells the main pattern owns.
 * <p>
 * The Nexus Terminal reads this list: a "module build" larger than the number of modules picks the alternative
 * with that rank (1 = the first) and builds only it.
 */
public interface IAlternativeStructureMachine {

    /** The alternative structures, in the order the terminal's module build count reaches them (may be empty). */
    List<BlockPattern> gtna$alternativeStructures();
}
