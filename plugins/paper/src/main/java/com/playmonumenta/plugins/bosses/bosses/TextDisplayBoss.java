package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TextDisplayBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_text_display";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Minimum distance to a player for the display to update")
		public int DETECTION = 50;
		@BossParam(help = "Interpolation duration")
		public int ANIMATION_TICKS = 1;

		@BossParam(help = "Display billboard mode")
		public Display.Billboard BILLBOARD = Display.Billboard.CENTER;
		@BossParam(help = "The text to be displayed in minimessage format")
		public String TEXT = "<red>I am EVIL.";
		@BossParam(help = "Whether the display glows")
		public boolean GLOWING = false;
		@BossParam(help = "Display glow color (glow_color_override nbt tag)")
		public String GLOW_COLOR = "ffffff";

		@BossParam(help = "Background color of the text")
		public String BACKGROUND_COLOR = "ffffff";

		@BossParam(help = "The opacity of the background. Between 0 (none) and 1 (full)")
		public double BACKGROUND_OPACITY = 1;

		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_X = 0;
		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_Y = 0;
		@BossParam(help = "Translation is affected by rotation")
		public float TRANSLATION_Z = 0;

		@BossParam(help = "Rotation about the X axis (degrees)")
		public float ROTATION_X = 0;
		@BossParam(help = "Rotation about the Y axis (degrees)")
		public float ROTATION_Y = 0;
		@BossParam(help = "Rotation about the Z axis (degrees)")
		public float ROTATION_Z = 0;

		@BossParam(help = "Should display rotate with the entity its on")
		public boolean FOLLOW_ENTITY_ROTATION = false;
		@BossParam(help = "Should display rotate without y")
		public boolean IGNORE_Y_ROTATION = false;

		@BossParam(help = "Scale of the display")
		public float SCALE_X = 1;
		@BossParam(help = "Scale of the display")
		public float SCALE_Y = 1;
		@BossParam(help = "Scale of the display")
		public float SCALE_Z = 1;
	}

	private final Display mDisplay;

	public TextDisplayBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mDisplay = boss.getWorld().spawn(boss.getLocation(), TextDisplay.class, d -> {
			d.setTransformation(new Transformation(
				new Vector3f(p.TRANSLATION_X, p.TRANSLATION_Y, p.TRANSLATION_Z),
				new Quaternionf().rotateYXZ((float) Math.toRadians(p.ROTATION_Y), (float) Math.toRadians(p.ROTATION_X), (float) Math.toRadians(p.ROTATION_Z)),
				new Vector3f(p.SCALE_X, p.SCALE_Y, p.SCALE_Z),
				new Quaternionf()
			));
			d.text(MessagingUtils.fromMiniMessage(p.TEXT));
			d.setBillboard(p.BILLBOARD);
			d.setTeleportDuration(Math.clamp(0, p.ANIMATION_TICKS, 59));
			if (p.GLOWING) {
				d.setGlowing(true);
			}

			int glowRgb = Integer.parseInt(p.GLOW_COLOR, 16); // e.g. "FF0000"
			d.setGlowColorOverride(Color.fromRGB(glowRgb));

			int backgroundRgb = Integer.parseInt(p.BACKGROUND_COLOR, 16);
			int backgroundArgb = (Math.clamp(Math.round(p.BACKGROUND_OPACITY * 255), 0, 255) << 24) | backgroundRgb;

			d.setBackgroundColor(Color.fromARGB(backgroundArgb));

			EntityUtils.setRemoveEntityOnUnload(d);
		});
		boss.addPassenger(mDisplay);

		super.constructBoss(SpellManager.EMPTY, p.FOLLOW_ENTITY_ROTATION ? List.of(new Spell() {
			@Override
			public void run() {
				mDisplay.setInterpolationDelay(0);
				mDisplay.setRotation(boss.getYaw(), p.IGNORE_Y_ROTATION ? 0 : boss.getPitch());
			}

			@Override
			public int cooldownTicks() {
				return 0;
			}
		}) : List.of(), p.DETECTION, null, 0, 1);
	}

	@Override
	public void unload() {
		super.unload();
		// If unload is triggered because the boss is dead, remove the display.
		// Otherwise, don't remove it - mDisplay has setPersistent(false) so it won't be saved
		if (mBoss.isDead()) {
			mDisplay.remove();
		}
	}
}
