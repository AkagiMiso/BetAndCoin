package com.example.betandcoin.client.renderer;

import com.example.betandcoin.client.model.CasinoFarkleTableItemModel;
import com.example.betandcoin.item.CasinoFarkleTableItem;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class CasinoFarkleTableItemRenderer extends GeoItemRenderer<CasinoFarkleTableItem> {
    public CasinoFarkleTableItemRenderer() {
        super(new CasinoFarkleTableItemModel());
    }
}
