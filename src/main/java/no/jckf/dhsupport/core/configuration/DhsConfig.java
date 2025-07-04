/*
 * DH Support, server-side support for Distant Horizons.
 * Copyright (C) 2024 Jim C K Flaten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package no.jckf.dhsupport.core.configuration;

public abstract class DhsConfig
{
    public static String CONFIG_VERSION = "config_version";

    public static String DEBUG = "debug";

    public static String RENDER_DISTANCE = "render_distance";

    public static String DISTANT_GENERATION_ENABLED = "distant_generation_enabled";

    public static String FULL_DATA_REQUEST_CONCURRENCY_LIMIT = "full_data_request_concurrency_limit";

    public static String REAL_TIME_UPDATES_ENABLED = "real_time_updates_enabled";

    public static String REAL_TIME_UPDATE_RADIUS = "real_time_update_radius";

    public static String LOGIN_DATA_SYNC_ENABLED = "login_data_sync_enabled";

    public static String LOGIN_DATA_SYNC_RADIUS = "login_data_sync_radius";

    public static String LOGIN_DATA_SYNC_RC_LIMIT = "login_data_sync_rc_limit";

    public static String MAX_DATA_TRANSFER_SPEED = "max_data_transfer_speed";

    public static String SCHEDULER_THREADS = "scheduler_threads";

    public static String LEVEL_KEY_PREFIX = "level_key_prefix";

    public static String BORDER_CENTER_X = "border_center_x";

    public static String BORDER_CENTER_Z = "border_center_z";

    public static String BORDER_RADIUS = "border_radius";
    
    public static String USE_VANILLA_WORLD_BORDER = "use_vanilla_world_border";
    
    public static String VANILLA_WORLD_BORDER_EXPANSION = "vanilla_world_border_expansion";

    public static String SHOW_BUILDER_ACTIVITY = "show_builder_activity";

    public static String GENERATE_NEW_CHUNKS = "generate_new_chunks";

    public static String GENERATE_NEW_CHUNKS_WARNING = "generate_new_chunks_warning";

    public static String BUILDER_TYPE = "builder_type";

    public static String BUILDER_RESOLUTION = "builder_resolution";

    public static String SCAN_TO_SEA_LEVEL= "scan_to_sea_level";

    public static String INCLUDE_NON_COLLIDING_TOP_LAYER = "include_non-colliding_top_layer";

    public static String PERFORM_UNDERGLOW_HACK = "perform_underglow_hack";

    public static String INCLUDE_BEACONS = "include_beacons";
}
