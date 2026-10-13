package com.example.betandcoin.item;

import com.example.betandcoin.client.renderer.CasinoFarkleTableItemRenderer;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

/** Placeable table item with a GeckoLib preview of the static main model. */
public class CasinoFarkleTableItem extends BlockItem implements IAnimatable {
    private final AnimationFactory factory = new AnimationFactory(this);

    public CasinoFarkleTableItem(Block block, Properties properties) {
        super(block, properties.setISTER(() -> CasinoFarkleTableItemRenderer::new));
    }

    @Override
    public void registerControllers(AnimationData data) {
        // The item preview is static; the placed table owns its animations.
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
