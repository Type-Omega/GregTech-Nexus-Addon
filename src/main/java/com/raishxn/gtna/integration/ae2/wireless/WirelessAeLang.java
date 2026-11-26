package com.raishxn.gtna.integration.ae2.wireless;

import com.raishxn.gtna.api.registry.GTNARegistry;

/** English texts of the wireless ME network (D-444); pt_br and zh_cn are in the lang files. */
final class WirelessAeLang {

    private WirelessAeLang() {}

    static void init() {
        lang("key.gtna.wireless_binding_picker",
                "Open Wireless Network Picker");
        lang("curios.identifier.wireless_connector",
                "Wireless Connector");
        lang("gtna.wireless.tooltip.tool.select",
                "Shift + right-click a Wireless ME Network Core to save its network");
        lang("gtna.wireless.tooltip.tool.pick",
                "Right-click the air to pick a network from a list, or press the Open Wireless Network Picker key (the tool can be in any slot or in the Wireless Connector Curios slot)");
        lang("gtna.wireless.tooltip.tool.bind",
                "Right-click an ME part, hatch, interface or pattern provider to bind it to the saved network");
        lang("gtna.wireless.tooltip.tool.rebind",
                "Shift + right-click an already bound block to replace its binding with the saved network");
        lang("gtna.wireless.tooltip.tool.place",
                "Sneak-placing a block while carrying the tool binds it to the saved network");
        lang("gtna.wireless.tooltip.tool.clear",
                "Left-click the air to clear the saved network; bound blocks stay connected");
        lang("gtna.wireless.tooltip.tool.disconnect",
                "Shift + left-click a bound block to unlink it; the tool keeps its saved network");
        lang("gtna.wireless.tooltip.tool.network",
                "Saved network: %s");
        lang("gtna.wireless.tooltip.tool.unknown_name",
                "Network name currently unavailable");
        lang("gtna.wireless.tooltip.tool.empty",
                "No network saved");
        lang("gtna.wireless.message.tool.already_bound",
                "This block already has a wireless binding; Shift + right-click to replace it");
        lang("gtna.wireless.message.tool.select_core",
                "Save a network first: Shift + right-click its core, or pick one with the picker");
        lang("gtna.wireless.message.tool.saved",
                "Saved wireless ME network: %s");
        lang("gtna.wireless.message.tool.cleared",
                "Cleared the wireless ME network saved in this tool");
        lang("gtna.wireless.message.tool.not_bound",
                "This block has no wireless ME network binding to remove");
        lang("gtna.wireless.message.tool.no_access",
                "You do not have permission to access this wireless ME network");
        lang("gtna.wireless.message.tool.existing_connection",
                "This block already has a wired or shared ME connection; disconnect it first");
        lang("gtna.wireless.message.target.disconnected",
                "Disconnected this ME block from its wireless network");
        lang("gtna.wireless.message.target.core_not_connected",
                "Place the Wireless ME Network Core against an existing ME cable or ME block first");
        lang("gtna.wireless.message.target.missing_core",
                "That wireless ME network core no longer exists");
        lang("gtna.wireless.message.target.invalid_target",
                "This block does not expose an ME network node");
        lang("gtna.wireless.message.target.linked_target",
                "Linked this block to the wireless ME network (%s)");
        lang("gtna.wireless.message.target.linked_target_pending",
                "Recorded this block and waiting for the wireless ME network connection (%s)");
        lang("gtna.wireless.message.target.machine_denied",
                "You cannot configure this machine: it belongs to another team");
        lang("gtna.wireless.message.core.name_saved",
                "Wireless ME network named: %s");
        lang("gtna.wireless.screen.picker",
                "Select Wireless ME Network");
        lang("gtna.wireless.screen.core",
                "Wireless ME Network Core");
        lang("gtna.wireless.label.core.ae_connected",
                "Status: attached to an ME network");
        lang("gtna.wireless.label.core.ae_disconnected",
                "Status: not attached to an ME network");
        lang("gtna.wireless.label.target.current_connected",
                "Current network: %s");
        lang("gtna.wireless.label.target.current_disconnected",
                "Current network: not connected");
        lang("gtna.wireless.label.target.no_networks",
                "No wireless ME networks are available");
        lang("gtna.wireless.field.core.name",
                "Network name");
        lang("gtna.wireless.button.core.save",
                "Save Network Name");
    }

    private static void lang(String key, String text) {
        GTNARegistry.REGISTRATE.addRawLang(key, text);
    }
}
