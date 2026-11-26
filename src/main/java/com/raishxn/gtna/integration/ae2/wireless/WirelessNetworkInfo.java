// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.GlobalPos;

import java.util.UUID;

/** A wireless network as shown in the picker: its id, its name and where its core stands. */
public record WirelessNetworkInfo(UUID frequency, String name, GlobalPos core) {}
