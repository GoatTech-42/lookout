package dev.meshac.tracker;

import dev.meshac.geometry.box.AxisAlignedBox;
import dev.meshac.geometry.box.EntitySize;
import java.util.EnumMap;
import java.util.UUID;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Server-side view of an entity the client knows about. Ported from Vulcan's TrackedEntity. */
public class TrackedEntity {

	private EntityType<?> entityType;
	private UUID uniqueId;
	private float yaw;
	private float pitch;
	private double x;
	private double y;
	private double z;
	private final int entityId;
	private EnumMap<EquipmentSlot, ItemStack> equipment = null;

	public TrackedEntity(int entityId) {
		this.entityId = entityId;
	}

	public void setEquipment(EquipmentSlot slot, ItemStack item) {
		if (item == ItemStack.EMPTY && this.getEquipment(slot) == ItemStack.EMPTY) return;
		if (this.equipment == null) this.equipment = new EnumMap<>(EquipmentSlot.class);
		this.equipment.put(slot, item);
	}

	public ItemStack getEquipment(EquipmentSlot slot) {
		if (this.equipment == null) return ItemStack.EMPTY;
		return this.equipment.getOrDefault(slot, ItemStack.EMPTY);
	}

	public boolean hasEquipment(EquipmentSlot slot) {
		if (this.equipment == null) return false;
		ItemStack item = this.equipment.get(slot);
		return item != null && !item.isEmpty();
	}

	public AxisAlignedBox getAxisAlignedBox() {
		return EntitySize.getEntitySize(this.entityType).getAxisAlignedBox(this.x, this.y, this.z);
	}

	public EntityType<?> getEntityType() { return this.entityType; }
	public UUID getUniqueId() { return this.uniqueId; }
	public float getYaw() { return this.yaw; }
	public float getPitch() { return this.pitch; }
	public double getX() { return this.x; }
	public double getY() { return this.y; }
	public double getZ() { return this.z; }
	public int getEntityId() { return this.entityId; }
	public EnumMap<EquipmentSlot, ItemStack> getEquipment() { return this.equipment; }
	public void setEntityType(EntityType<?> entityType) { this.entityType = entityType; }
	public void setUniqueId(UUID uniqueId) { this.uniqueId = uniqueId; }
	public void setYaw(float yaw) { this.yaw = yaw; }
	public void setPitch(float pitch) { this.pitch = pitch; }
	public void setX(double x) { this.x = x; }
	public void setY(double y) { this.y = y; }
	public void setZ(double z) { this.z = z; }
	public void setEquipment(EnumMap<EquipmentSlot, ItemStack> equipment) { this.equipment = equipment; }
}
