package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.managers.PlayerSkinManager;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scoreboard.Team;

// Fake players are forced to count for Duelist ("humanlike")
public class FakePlayerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_player";
	public static final String DEFAULT_TEAM = "_fake_player_default";

	public static class Parameters extends BossParameters {
		@BossParam(help = "friendly name of the skin to apply like 'villager174'")
		public PlayerSkinManager.SkinData SKIN_NAME = PlayerSkinManager.fetchFallbackSkin();
		@BossParam(help = "display armor if set to true")
		public boolean SHOW_ARMOR = true;
		@BossParam(help = "override PEB, for what? ask USB idk")
		public boolean OVERRIDE_PEB = false;
		@BossParam(help = "add to team")
		public boolean TEAM = true;
	}

	public FakePlayerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters params = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (params.TEAM) {
			Team team = ScoreboardUtils.getExistingTeamOrCreate(DEFAULT_TEAM);
			team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
			team.addEntity(boss);
		}

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 0, null);
	}

	public static boolean is(Entity entity) {
		return entity.getScoreboardTags().contains(identityTag);
	}

	public static String convertIdToFakeName(int entityId) {
		return "|npc_" + Integer.toString(entityId, 36);
	}
}
